from __future__ import annotations

import httpx
import pytest
import respx

from ai_bot.poller import EventPoller

BASE = "http://test-backend/api/v1"


@pytest.fixture
def poller(monkeypatch: pytest.MonkeyPatch) -> EventPoller:
    monkeypatch.setenv("AI_BOT_BACKEND_URL", BASE)
    monkeypatch.setenv("AI_BOT_OLLAMA_URL", "http://test-ollama:11434")
    monkeypatch.setenv("AI_BOT_LLM_TYPE", "ollama")
    from ai_bot.config import settings
    # Globales Singleton für den Test umbiegen + danach restaurieren
    # (sonst leakt der Test State in andere Tests).
    old = (settings.backend_url, settings.ollama_url, settings.llm_type)
    settings.backend_url = BASE
    settings.ollama_url = "http://test-ollama:11434"
    settings.llm_type = "ollama"
    try:
        yield EventPoller()
    finally:
        settings.backend_url, settings.ollama_url, settings.llm_type = old


@respx.mock
async def test_tick_skips_campaigns_with_bot_mode_off(poller: EventPoller) -> None:
    respx.get(f"{BASE}/bot/worlds").mock(return_value=httpx.Response(200, json=[
        {"worldId": "w1", "worldAiMode": "suggest",
         "campaigns": [{"id": "c1", "botMode": "off"}]},
    ]))
    events_route = respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json=[])
    )

    await poller.tick()

    assert events_route.call_count == 0  # Kampagne off → Welt uebersprungen


@respx.mock
async def test_tick_polls_world_with_allowed_campaign(poller: EventPoller) -> None:
    respx.get(f"{BASE}/bot/worlds").mock(return_value=httpx.Response(200, json=[
        {"worldId": "w1", "worldAiMode": "suggest",
         "campaigns": [{"id": "c1", "botMode": "suggest"}]},
    ]))
    events_route = respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json=[])
    )

    await poller.tick()

    assert events_route.call_count == 1


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


@respx.mock
async def test_tick_bad_world_does_not_starve_others(poller: EventPoller) -> None:
    """TDD (CRITICAL 4): Eine fehlerhafte Welt darf andere Welten nicht blockieren."""
    respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[{"id": "w-bad"}, {"id": "w-good"}])
    )
    respx.get(f"{BASE}/worlds/w-bad/events?since=0&limit=50").mock(
        return_value=httpx.Response(500)
    )
    respx.get(f"{BASE}/worlds/w-good/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json={"events": [{"id": 7, "event_type": "TEST", "source_entity_id": None}]})
    )
    await poller.tick()
    assert poller.last_ids.get("w-good") == 7
    assert "w-bad" not in poller.last_ids


@respx.mock
async def test_tick_bad_event_retries_next_tick(
    poller: EventPoller, monkeypatch: pytest.MonkeyPatch
) -> None:
    """TDD (CRITICAL 4): Fehlerhaftes Event stoppt den Batch, last_ids bleibt
    stehen, nächster Tick holt es erneut (at-least-once)."""
    respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[{"id": "w1"}])
    )
    respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json={"events": [
            {"id": 1, "event_type": "TEST", "source_entity_id": None},
            {"id": 2, "event_type": "TEST", "source_entity_id": None},
        ]})
    )
    orig = poller.handle_event
    seen: list[int] = []

    async def flaky(world_id: str, event: dict) -> None:
        seen.append(event["id"])
        if event["id"] == 1 and seen.count(1) == 1:
            raise RuntimeError("boom")
        await orig(world_id, event)

    monkeypatch.setattr(poller, "handle_event", flaky)
    await poller.tick()
    assert seen == [1]
    assert poller.last_ids.get("w1", 0) == 0
    await poller.tick()
    assert poller.last_ids.get("w1") == 2


def test_apply_token_budget_truncates() -> None:
    """TDD: context_max_tokens muss als Zeichen-Budget (4 Zeichen/Token)
    durchgesetzt werden."""
    from ai_bot.poller import _apply_token_budget
    long_text = "x" * 10000
    out = _apply_token_budget(long_text, 100)
    assert len(out) <= 400 + 100
    assert "gekürzt" in out
    short = "kurz"
    assert _apply_token_budget(short, 100) == short
