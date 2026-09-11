"""Tests für last_ids-Persistenz (AI-AGENT.md §3.3)."""
from __future__ import annotations

import httpx
import pytest
import respx

from ai_bot.llm_client import MockLLMClient
from ai_bot.poller import EventPoller
from ai_bot.state import StateStore

BASE = "http://test-backend/api/v1"


def test_state_roundtrip(tmp_path) -> None:
    state = StateStore(tmp_path / "state.db")
    assert state.load() == {}
    state.save({"w1": 5, "w2": 9})
    assert state.load() == {"w1": 5, "w2": 9}
    state.save({"w1": 7})
    assert state.load() == {"w1": 7, "w2": 9}
    state.close()


def test_poller_loads_persisted_offsets(tmp_path) -> None:
    state = StateStore(tmp_path / "state.db")
    state.save({"w1": 42})
    poller = EventPoller(state=state)
    assert poller.last_ids == {"w1": 42}


@respx.mock
async def test_tick_persists_offsets(tmp_path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_BOT_BACKEND_URL", BASE)
    from ai_bot.config import settings
    settings.backend_url = BASE

    state = StateStore(tmp_path / "state.db")
    poller = EventPoller(state=state, llm=MockLLMClient({"action": "IDLE", "reasoning": "mock"}))

    respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[{"id": "w1"}])
    )
    respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json={"events": [{"id": 1, "event_type": "MOVED", "source_entity_id": "e1"}]})
    )
    respx.get(f"{BASE}/worlds/w1/entities/e1").mock(
        return_value=httpx.Response(200, json={
            "id": "e1", "name": "Test NPC", "entity_type": "npc",
            "metadata_json": {"personality": "neutral", "location_id": None, "language": "de"},
        })
    )
    respx.get(f"{BASE}/worlds/w1/entities?limit=10").mock(
        return_value=httpx.Response(200, json=[])
    )
    respx.get(f"{BASE}/entity-events?entityType=npc&entityId=e1&limit=5").mock(
        return_value=httpx.Response(200, json=[])
    )
    respx.get(f"{BASE}/entities/e1/memories").mock(
        return_value=httpx.Response(200, json=[])
    )
    respx.post(f"{BASE}/npc-intents").mock(
        return_value=httpx.Response(201, json={"id": "int1"})
    )

    await poller.tick()

    assert poller.last_ids == {"w1": 1}
    assert state.load() == {"w1": 1}
