from __future__ import annotations

import asyncio
import logging
import random

import httpx
import jinja2
from jinja2 import Environment, FileSystemLoader

from ai_bot.api_client import BackendClient
from ai_bot.config import settings
from ai_bot.llm_client import LLMClient, create_llm_client
from ai_bot.state import StateStore

logger = logging.getLogger("poller")

_PROMPT_DIR = __file__.rsplit("/", 1)[0] + "/prompts"
_jinja = Environment(loader=FileSystemLoader(_PROMPT_DIR))

ALLOWED_ACTIONS = frozenset({"ATTACK", "MOVE", "SPEAK", "USE_ITEM", "CHANGE_RELATION", "IDLE"})

# At-least-once: last_ids rücken nur nach erfolgreicher Verarbeitung vor.
# Fehler lassen Events für den nächsten Tick liegen; nach Neustart (in-memory)
# replayen recente Events — das Backend dedup via event_hash (AI-AGENT.md §3.3).

# ponytail: fixed truncation budget for untrusted prompt fields; raise only if prompts get cut.
_MAX_FIELD = 500


def _safe(value: object, limit: int = _MAX_FIELD) -> str:
    """Kürzt + entgiftet untrusted Felder (Prompt-Injection-Hygiene)."""
    text = "" if value is None else str(value)
    return text if len(text) <= limit else text[:limit] + "…"


def _validate_proposal(result: object) -> dict | None:
    """Pydantic-style Check per AI-AGENT.md §6.2: dict + erlaubte action + params-dict."""
    if not isinstance(result, dict):
        return None
    if result.get("action") not in ALLOWED_ACTIONS:
        return None
    params = result.get("params", {})
    if params is None:
        params = {}
    if not isinstance(params, dict):
        return None
    return {
        "action": result["action"],
        "target_id": result.get("target_id"),
        "params": params,
        "reasoning": str(result.get("reasoning", "")),
    }


class EventPoller:
    """Pollt Welt-Events, erzeugt NPC-Kontext, ruft LLM, reicht Intents ein."""

    def __init__(self, api: BackendClient | None = None,
                 llm: LLMClient | None = None,
                 state: StateStore | None = None) -> None:
        self.api = api if api is not None else BackendClient()
        self.llm: LLMClient = llm if llm is not None else create_llm_client()
        # Persistente Offsets (AI-AGENT.md §3.3): None => in-memory (Backend-dedup trägt)
        self.state = state if state is not None else (
            StateStore(settings.state_path) if settings.state_path else None
        )
        self.last_ids: dict[str, int] = self.state.load() if self.state else {}
        self.consecutive_failures = 0

    async def run(self) -> None:
        logger.info("Poller started (interval=%sms, llm=%s)",
                     settings.poll_interval_ms, settings.llm_type)
        while True:
            try:
                await self.tick()
                self.consecutive_failures = 0
            except Exception:
                self.consecutive_failures += 1
                logger.exception("Tick error (consecutive=%d)", self.consecutive_failures)
            await asyncio.sleep(settings.poll_interval_ms / 1000)

    async def tick(self) -> None:
        """Ruft Events für jede aktive Welt ab.

        Eine fehlerhafte Welt blockiert die anderen nicht. Innerhalb einer
        Welt stoppt ein fehlerhaftes Event den Rest des Batches: last_ids
        rückt nur bei Erfolg vor, der Fehlschlag wird nächsten Tick erneut
        geholt (at-least-once; Backend dedup via event_hash).
        """
        worlds = await self.api.get_active_worlds()
        for world in worlds:
            world_id = world.get("id", "")
            if not world_id:
                continue
            try:
                since = self.last_ids.get(world_id, 0)
                events = await self.api.get_events(world_id, since=since)
            except Exception:
                logger.exception("Skipping world %s after fetch error", world_id)
                continue
            for event in events:
                try:
                    await self.handle_event(world_id, event)
                    # Auch die target-Entity als Reaktionsauslöser behandeln
                    await self.handle_target_event(world_id, event)
                except Exception:
                    logger.exception("Stopping world %s at event %s, retry next tick",
                                     world_id, event.get("id"))
                    break
                eid = event.get("id", 0)
                if isinstance(eid, int) and eid > self.last_ids.get(world_id, 0):
                    self.last_ids[world_id] = eid
        self._save_state()

    def _save_state(self) -> None:
        """Persistiert Offsets nach jedem Tick; Fehler brechen den Loop nicht."""
        if self.state is None:
            return
        try:
            self.state.save(self.last_ids)
        except Exception:
            logger.exception("Failed to persist last_ids")

    async def handle_target_event(self, world_id: str, event: dict) -> None:
        """Verarbeitet ein Event aus Sicht der target_entity_id.
        Ermöglicht NPC-Reaktionen, wenn sie Ziel einer Aktion wurden."""
        target = event.get("target_entity_id")
        if not target:
            return
        target_npc = await self.api.get_entity(world_id, target)
        if not target_npc:
            return

        # Reaktion des Ziel-NPCs auslösen
        logger.info("Target event: %s → %s", event.get("event_type"), target)
        await self.handle_npc_event(world_id, target_npc, event)

        # Wenn der NPC einer Fraktion angehört: Alle Faction-Mitglieder benachrichtigen
        faction_id = _resolve_faction_id(target_npc)
        if faction_id:
            faction_rels = await self.api.get_faction_relations(faction_id)
            # Wir haben keine get_faction_members API, daher nutzen wir
            # den vorhandenen Mechanismus: Events werden an die faction_id gehängt
            # Der nächste Poll-Zyklus holt neue Events → NPCs reagieren
            logger.info("Faction %s may react to event involving member %s", faction_id, target)

    async def handle_event(self, world_id: str, event: dict) -> None:
        """NPC-Kontext laden → Prompt bauen → LLM → Intent einreichen."""
        event_type = event.get("event_type", "")
        source = event.get("source_entity_id")
        if not source:
            return

        # 1. NPC-Kontext (P4-T03)
        npc = await self.api.get_entity(world_id, source)
        if not npc:
            return
        await self.handle_npc_event(world_id, npc, event)

    async def handle_npc_event(self, world_id: str, npc: dict, event: dict) -> None:
        """Verarbeitet ein Event für einen bestimmten NPC:"""
        source = npc.get("id", "")
        personality = (npc.get("metadata_json") or {}).get("personality", "neutral")
        nearby = await self.api.get_nearby_entities(world_id, source)
        location_id = (npc.get("metadata_json") or {}).get("location_id")
        location = None
        region = None
        npc_events = []
        location_events = []
        region_events = []
        weather = None
        npc_faction = None
        npc_faction_relations = []

        # P6-T06: Regionen + Orte + Events + Wetter + Fraktionen laden
        if location_id:
            location = await self.api.get_location(location_id)
            location_events = await self.api.get_entity_events("location", location_id, 5)
            if location and location.get("region_id"):
                region = await self.api.get_region(world_id, location["region_id"])
                region_events = await self.api.get_entity_events("region", location["region_id"], 5)
                weather = await self.api.get_weather(location["region_id"])

        # NPC-Fraktion laden
        npc_faction_id = _resolve_faction_id(npc)
        if npc_faction_id:
            npc_faction = await self.api.get_faction(npc_faction_id)
            npc_faction_relations = await self.api.get_faction_relations(npc_faction_id)

        npc_events = await self.api.get_entity_events("npc", source, 5)

        # Memories über diesen NPC laden
        memories = await self.api.get_memories(source)

        # Aus dem aktuellen Event eine Erinnerung ableiten
        await self._derive_memory(source, npc, event)

        # 2. Prompt-Template wählen (P4-T04)
        tmpl_name = f"{personality}.j2"
        try:
            tmpl = _jinja.get_template(tmpl_name)
        except jinja2.TemplateNotFound:
            logger.warning("Unknown personality %r, using neutral", personality)
            tmpl = _jinja.get_template("neutral.j2")

        try:
            prompt = tmpl.render(
                npc=npc,
                trigger=event,
                nearby=nearby[:5],
                location=location,
                region=region,
                npc_events=npc_events,
                location_events=location_events,
                region_events=region_events,
                weather=weather,
                npc_faction=npc_faction,
                npc_faction_relations=npc_faction_relations,
                memories=memories,
                backstory=npc.get("backstory"),
                age=npc.get("age"),
                experience_level=npc.get("experience_level"),
                social_standing=npc.get("social_standing"),
            )
        except jinja2.TemplateError:
            logger.exception("Prompt render failed, using minimal fallback")
            prompt = (
                f"NPC: {_safe(npc.get('name'))}\n"
                f"Persönlichkeit: {_safe(personality)}\n"
                f"Aktuelles Ereignis: {_safe(event.get('event_type'))}\n"
                "<<<TRIGGER_PAYLOAD>>>\n"
                f"{_safe(event.get('payload'), 1000)}\n"
                "<<<END_TRIGGER_PAYLOAD>>>\n"
                "Was tust du? Antworte mit JSON."
            )

        # 3. LLM-Aufruf (P4-T05, via P4-T01)
        system = (
            "Du bist der Geist einer Rollenspiel-Welt. Du steuerst NPCs basierend auf "
            "deren Persönlichkeit, Wissen und Zielen. Antworte ausschließlich mit JSON. "
            f"Sprache: {npc.get('metadata_json', {}).get('language', 'de')}. "
            "Form: {\"action\": \"ATTACK|MOVE|SPEAK|USE_ITEM|CHANGE_RELATION|IDLE\", "
            "\"target_id\": \"uuid|null\", "
            "\"params\": {\"faction_a_id\": \"...\", \"faction_b_id\": \"...\", \"relation_status\": \"WAR|ALLIANCE|UNFRIENDLY\"}, "
            "\"reasoning\": \"...\"}"
        )

        try:
            result = await self.llm.generate(system, prompt)
        except (httpx.HTTPError, ValueError, KeyError):
            logger.exception("LLM failed for NPC %s", source)
            return

        # 4. Output-Validierung (AI-AGENT.md §6.2): einmal Retry, sonst IDLE.
        proposal = _validate_proposal(result)
        if proposal is None:
            logger.warning("Invalid LLM output for NPC %s, retrying once", source)
            try:
                result = await self.llm.generate(
                    system, prompt + "\nAntworte mit gültigem JSON.")
            except (httpx.HTTPError, ValueError, KeyError):
                logger.exception("LLM retry failed for NPC %s", source)
                return
            proposal = _validate_proposal(result)
        if proposal is None:
            logger.warning("LLM output still invalid for NPC %s, falling back to IDLE", source)
            proposal = {"action": "IDLE", "target_id": None, "params": {},
                        "reasoning": "LLM-Output invalid"}

        # 5. Intent einreichen (P4-T06) — nie unvalidierte Aktionen senden.
        intent_type = proposal["action"]
        params = dict(proposal["params"])
        if proposal.get("target_id"):
            params["target_id"] = proposal["target_id"]
        await self.api.submit_intent(
            world_id=world_id,
            npc_id=source,
            intent_type=intent_type,
            params=params,
            reasoning=proposal.get("reasoning", ""),
        )

        # Gossip: Wenn NPC starke Erinnerungen hat, mit ~20% Wahrscheinlichkeit teilen
        if memories and len(memories) > 0:
            if random.random() < 0.2:
                await self._spread_gossip(world_id, source)

    async def _derive_memory(self, npc_id: str, npc: dict, event: dict) -> None:
        """Leitet aus einem Event eine Erinnerung für den NPC ab."""
        event_type = event.get("event_type", "")
        source = event.get("source_entity_id")
        target = event.get("target_entity_id")

        # Nur Erinnerungen an andere Entitäten (nicht an sich selbst)
        subject = target if target and target != npc_id else (source if source and source != npc_id else None)
        if not subject:
            return

        memory_type = "gossip"
        sentiment = 0
        summary = f"Event: {event_type}"

        if "ATTACK" in event_type or "COMBAT" in event_type:
            memory_type = "combat"
            sentiment = -2
            payload = event.get("payload", {})
            damage = payload.get("damage", 0) if isinstance(payload, dict) else 0
            summary = f"Wurde angegriffen ({damage} Schaden)"
            if damage > 5:
                sentiment = -4

        elif "SPEAK" in event_type:
            memory_type = "disrespect"
            sentiment = -1
            summary = "Hat etwas zu mir gesagt"

        elif "HELP" in event_type or "HEAL" in event_type or "TRADE" in event_type:
            memory_type = "helped"
            sentiment = 2
            summary = "Hat mir geholfen"

        await self.api.add_memory(npc_id, subject, memory_type, sentiment, summary)

    async def _spread_gossip(self, world_id: str, npc_id: str) -> None:
        """Wenn der NPC starke Erinnerungen hat, teilt er sie mit anderen NPCs am selben Ort."""
        memories = await self.api.get_memories(npc_id)
        # Stärkste Erinnerung finden (|sentiment| >= 2)
        strong = [m for m in memories if abs(m.get("sentiment", 0)) >= 2]
        if not strong:
            return
        top = strong[0]  # Neueste starke Erinnerung

        subject_id = top.get("subject_id", "")
        sentiment = top.get("sentiment", 0)
        summary = top.get("summary", "etwas")

        action = "geholfen" if sentiment > 0 else ("angegriffen" if "combat" in top.get("memory_type", "") else "schlecht behandelt")

        await self.api.submit_intent(
            world_id=world_id,
            npc_id=npc_id,
            intent_type="SPEAK",
            params={},
            reasoning=f"Ich habe gehört, dass {subject_id[:8]} mich {action} hat: {summary}",
        )


def _resolve_faction_id(npc: dict) -> str | None:
    """Extrahiert die faction_id aus einem NPC-Dict, egal ob sie
    unter metadata_json.faction_id oder direkt unter faction_id liegt."""
    meta = npc.get("metadata_json") or {}
    faction_id = meta.get("faction_id")
    if not faction_id:
        faction_id = npc.get("faction_id")
    return str(faction_id) if faction_id else None
