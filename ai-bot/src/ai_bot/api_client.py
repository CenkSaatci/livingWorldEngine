"""
HTTP-Client für das LWE-Backend (Spring Boot).
"""
from __future__ import annotations

import logging

import httpx
from src.ai_bot.config import settings

logger = logging.getLogger("api_client")


class BackendClient:
    def __init__(self) -> None:
        self.base = settings.backend_url
        self.headers = {
            "Content-Type": "application/json",
        }
        if settings.service_token:
            self.headers["Authorization"] = f"Bearer {settings.service_token}"

    async def get_active_worlds(self) -> list[dict]:
        """Liefert alle aktiven Welten (zur Polling-Planung)."""
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/worlds",
                    headers=self.headers,
                    timeout=10,
                )
                return resp.json() if resp.status_code == 200 else []
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/worlds", e)
                return []

    async def get_events(self, world_id: str, since: int = 0) -> list[dict]:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/worlds/{world_id}/events",
                    params={"since": since, "limit": 50},
                    headers=self.headers,
                    timeout=10,
                )
                if resp.status_code == 200:
                    data = resp.json()
                    return data.get("events", [])
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/worlds/{world_id}/events", e)
                pass
            return []

    async def get_entity(self, world_id: str, entity_id: str) -> dict | None:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/worlds/{world_id}/entities/{entity_id}",
                    headers=self.headers,
                    timeout=10,
                )
                return resp.json() if resp.status_code == 200 else None
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/worlds/{world_id}/entities/{entity_id}", e)
                return None

    async def get_nearby_entities(self, world_id: str, entity_id: str) -> list[dict]:
        """Vereinfachte Umgebungsabfrage."""
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/worlds/{world_id}/entities",
                    params={"limit": 10},
                    headers=self.headers,
                    timeout=10,
                )
                return resp.json() if resp.status_code == 200 else []
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/worlds/{world_id}/entities", e)
                return []

    async def get_location(self, location_id: str) -> dict | None:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/locations/{location_id}",
                    headers=self.headers, timeout=10)
                return resp.json() if resp.status_code == 200 else None
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/locations/{location_id}", e)
                return None

    async def get_region(self, world_id: str, region_id: str) -> dict | None:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/worlds/{world_id}/regions/{region_id}",
                    headers=self.headers, timeout=10)
                return resp.json() if resp.status_code == 200 else None
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/worlds/{world_id}/regions/{region_id}", e)
                return None

    async def get_entity_events(self, entity_type: str, entity_id: str, limit: int = 5) -> list[dict]:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/entity-events",
                    params={"entityType": entity_type, "entityId": entity_id, "limit": limit},
                    headers=self.headers, timeout=10)
                return resp.json() if resp.status_code == 200 else []
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/entity-events", e)
                return []

    async def get_weather(self, region_id: str) -> dict | None:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/regions/{region_id}/weather",
                    headers=self.headers, timeout=10)
                return resp.json() if resp.status_code == 200 else None
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/regions/{region_id}/weather", e)
                return None

    async def get_faction(self, faction_id: str) -> dict | None:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/factions/{faction_id}",
                    headers=self.headers, timeout=10)
                return resp.json() if resp.status_code == 200 else None
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/factions/{faction_id}", e)
                return None

    async def get_faction_relations(self, faction_id: str) -> list[dict]:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/factions/{faction_id}/relations",
                    headers=self.headers, timeout=10)
                return resp.json() if resp.status_code == 200 else []
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/factions/{faction_id}/relations", e)
                return []

    async def add_memory(self, entity_id: str, subject_id: str, memory_type: str,
                          sentiment: int, summary: str) -> bool:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.post(
                    f"{self.base}/entities/{entity_id}/memories",
                    json={"subjectId": subject_id, "memoryType": memory_type,
                          "sentiment": sentiment, "summary": summary},
                    headers=self.headers, timeout=10)
                return resp.status_code == 200
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/entities/{entity_id}/memories", e)
                return False

    async def get_memories(self, entity_id: str) -> list[dict]:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.get(
                    f"{self.base}/entities/{entity_id}/memories",
                    headers=self.headers, timeout=10)
                return resp.json() if resp.status_code == 200 else []
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/entities/{entity_id}/memories", e)
                return []

    async def submit_intent(
        self, world_id: str, npc_id: str, intent_type: str,
        params: dict, reasoning: str,
    ) -> dict | None:
        async with httpx.AsyncClient() as client:
            try:
                resp = await client.post(
                    f"{self.base}/npc-intents",
                    json={
                        "world_id": world_id,
                        "npc_id": npc_id,
                        "intent_type": intent_type,
                        "params_json": params,
                        "reasoning": reasoning,
                    },
                    headers=self.headers,
                    timeout=10,
                )
                return resp.json() if resp.status_code == 201 else None
            except httpx.HTTPError as e:
                logger.warning("Failed to call %s: %s", f"{self.base}/npc-intents", e)
                return None
