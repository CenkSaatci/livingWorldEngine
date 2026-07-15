from __future__ import annotations

import httpx
import pytest
import respx

from src.ai_bot.poller import EventPoller

BASE = "http://test-backend/api/v1"


@pytest.fixture
def poller(monkeypatch: pytest.MonkeyPatch) -> EventPoller:
    monkeypatch.setenv("AI_BOT_BACKEND_URL", BASE)
    monkeypatch.setenv("AI_BOT_OLLAMA_URL", "http://test-ollama:11434")
    monkeypatch.setenv("AI_BOT_LLM_TYPE", "ollama")
    from src.ai_bot.config import settings
    settings.backend_url = BASE
    settings.ollama_url = "http://test-ollama:11434"
    settings.llm_type = "ollama"
    return EventPoller()


@respx.mock
async def test_tick_no_worlds(poller: EventPoller) -> None:
    respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[])
    )
    await poller.tick()


@respx.mock
async def test_tick_with_event_no_source(poller: EventPoller) -> None:
    respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[{"id": "w1"}])
    )
    respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json={"events": [{"id": 1, "event_type": "TEST", "source_entity_id": None}]})
    )
    await poller.tick()


@respx.mock
async def test_tick_without_npc(poller: EventPoller) -> None:
    respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[{"id": "w1"}])
    )
    respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json={"events": [{"id": 1, "event_type": "TEST", "source_entity_id": "e1"}]})
    )
    respx.get(f"{BASE}/worlds/w1/entities/e1").mock(return_value=httpx.Response(404))
    await poller.tick()


@respx.mock
async def test_tick_full_flow(poller: EventPoller) -> None:
    npc = {
        "id": "e1", "name": "Test NPC", "entity_type": "npc",
        "metadata_json": {
            "personality": "neutral",
            "location_id": None,
            "language": "de",
        },
    }
    # worlds → events → entity → nearby → location (none) → LLM → submit
    respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[{"id": "w1"}])
    )
    respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json={"events": [{"id": 1, "event_type": "MOVED", "source_entity_id": "e1"}]})
    )
    respx.get(f"{BASE}/worlds/w1/entities/e1").mock(
        return_value=httpx.Response(200, json=npc)
    )
    respx.get(f"{BASE}/worlds/w1/entities?limit=10").mock(
        return_value=httpx.Response(200, json=[])
    )
    respx.get(f"{BASE}/entity-events?entityType=npc&entityId=e1&limit=5").mock(
        return_value=httpx.Response(200, json=[])
    )
    # Memories
    respx.get(f"{BASE}/entities/e1/memories").mock(
        return_value=httpx.Response(200, json=[])
    )
    # LLM call
    respx.post("http://test-ollama:11434/api/generate").mock(
        return_value=httpx.Response(200, json={"response": '{"action": "IDLE", "reasoning": "nothing to do"}'})
    )
    # intent submit
    respx.post(f"{BASE}/npc-intents").mock(
        return_value=httpx.Response(201, json={"id": "int1"})
    )
    await poller.tick()
