from __future__ import annotations

import asyncio
import json
import logging

import httpx
from jinja2 import Environment, FileSystemLoader

from src.ai_bot.config import settings
from src.ai_bot.api_client import BackendClient
from src.ai_bot.llm_client import create_llm_client, LLMClient

logger = logging.getLogger("poller")

_PROMPT_DIR = __file__.rsplit("/", 1)[0] + "/prompts"
_jinja = Environment(loader=FileSystemLoader(_PROMPT_DIR))


class EventPoller:
    """Pollt Welt-Events, erzeugt NPC-Kontext, ruft LLM, reicht Intents ein."""

    def __init__(self) -> None:
        self.api = BackendClient()
        self.llm: LLMClient = create_llm_client()
        self.last_ids: dict[str, int] = {}

    async def run(self) -> None:
        logger.info("Poller started (interval=%sms, llm=%s)",
                     settings.poll_interval_ms, settings.llm_type)
        while True:
            try:
                await self.tick()
            except Exception:
                logger.exception("Tick error")
            await asyncio.sleep(settings.poll_interval_ms / 1000)

    async def tick(self) -> None:
        """Ruft Events für jede aktive Welt ab."""
        worlds = await self.api.get_active_worlds()
        for world in worlds:
            world_id = world.get("id", "")
            since = self.last_ids.get(world_id, 0)
            events = await self.api.get_events(world_id, since=since)
            for event in events:
                await self.handle_event(world_id, event)
                eid = event.get("id", 0)
                if eid > self.last_ids.get(world_id, 0):
                    self.last_ids[world_id] = eid

    async def handle_event(self, world_id: str, event: dict) -> None:
        """NPC-Kontext laden → Prompt bauen → LLM → Intent einreichen."""
        event_type = event.get("event_type", "")
        source = event.get("source_entity_id")
        if not source:
            return

        # 1. NPC-Kontext (P4-T03)
        npc = await self.api.get_entity(source)
        if not npc:
            return
        personality = (npc.get("metadata_json") or {}).get("personality", "neutral")
        nearby = await self.api.get_nearby_entities(world_id, source)
        location_id = (npc.get("metadata_json") or {}).get("location_id")
        location = None
        region = None
        npc_events = []
        location_events = []
        region_events = []

        # P6-T06: Regionen + Orte + Events laden
        if location_id:
            location = await self.api.get_location(location_id)
            location_events = await self.api.get_entity_events("location", location_id, 5)
            if location and location.get("region_id"):
                region = await self.api.get_region(location["region_id"])
                region_events = await self.api.get_entity_events("region", location["region_id"], 5)
        npc_events = await self.api.get_entity_events("npc", source, 5)

        # 2. Prompt-Template wählen (P4-T04)
        tmpl_name = f"{personality}.j2"
        try:
            tmpl = _jinja.get_template(tmpl_name)
        except Exception:
            tmpl = _jinja.get_template("neutral.j2")

        prompt = tmpl.render(
            npc=npc,
            trigger=event,
            nearby=nearby[:5],
            location=location,
            region=region,
            npc_events=npc_events,
            location_events=location_events,
            region_events=region_events,
            language=(npc.get("metadata_json") or {}).get("language", "de"),
        )

        # 3. LLM-Aufruf (P4-T05, via P4-T01)
        system = (
            "Du bist der Geist einer Rollenspiel-Welt. Du steuerst NPCs basierend auf "
            "deren Persönlichkeit, Wissen und Zielen. Antworte ausschließlich mit JSON. "
            f"Sprache: {npc.get('metadata_json', {}).get('language', 'de')}. "
            "Form: {\"action\": \"ATTACK|MOVE|SPEAK|USE_ITEM|IDLE\", "
            "\"target_id\": \"uuid|null\", \"reasoning\": \"...\"}"
        )

        try:
            result = await self.llm.generate(system, prompt)
        except Exception:
            logger.exception("LLM failed for NPC %s", source)
            return

        # 4. Intent einreichen (P4-T06)
        intent_type = result.get("action", "IDLE")
        params = {"target_id": result.get("target_id")} if result.get("target_id") else {}
        await self.api.submit_intent(
            world_id=world_id,
            npc_id=source,
            intent_type=intent_type,
            params=params,
            reasoning=result.get("reasoning", ""),
        )
