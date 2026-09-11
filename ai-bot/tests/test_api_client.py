from __future__ import annotations

import json
import httpx
import pytest
import respx

from ai_bot.api_client import BackendClient

BASE = "http://test-backend/api/v1"
TOKEN = "test-token"


@pytest.fixture
def client(monkeypatch: pytest.MonkeyPatch) -> BackendClient:
    monkeypatch.setenv("AI_BOT_BACKEND_URL", BASE)
    monkeypatch.setenv("AI_BOT_SERVICE_TOKEN", TOKEN)
    from ai_bot.config import settings
    _old = settings.backend_url, settings.service_token
    settings.backend_url = BASE
    settings.service_token = TOKEN
    return BackendClient()


@respx.mock
async def test_get_active_worlds_ok(client: BackendClient) -> None:
    route = respx.get(f"{BASE}/worlds").mock(
        return_value=httpx.Response(200, json=[{"id": "w1"}, {"id": "w2"}])
    )
    result = await client.get_active_worlds()
    assert result == [{"id": "w1"}, {"id": "w2"}]
    assert route.called


@respx.mock
async def test_get_active_worlds_error(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds").mock(return_value=httpx.Response(500))
    result = await client.get_active_worlds()
    assert result == []


@respx.mock
async def test_get_active_worlds_timeout(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds").mock(side_effect=httpx.TimeoutException("timeout"))
    result = await client.get_active_worlds()
    assert result == []


@respx.mock
async def test_get_events_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(200, json={"events": [{"id": 1}]})
    )
    result = await client.get_events("w1")
    assert result == [{"id": 1}]


@respx.mock
async def test_get_events_error(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds/w1/events?since=0&limit=50").mock(
        return_value=httpx.Response(404)
    )
    result = await client.get_events("w1")
    assert result == []


@respx.mock
async def test_get_entity_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds/w1/entities/e1").mock(
        return_value=httpx.Response(200, json={"id": "e1", "name": "Goblin"})
    )
    result = await client.get_entity("w1", "e1")
    assert result == {"id": "e1", "name": "Goblin"}


@respx.mock
async def test_get_entity_not_found(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds/w1/entities/e1").mock(return_value=httpx.Response(404))
    result = await client.get_entity("w1", "e1")
    assert result is None


@respx.mock
async def test_get_nearby_entities(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds/w1/entities?limit=10").mock(
        return_value=httpx.Response(200, json=[{"id": "e2"}])
    )
    result = await client.get_nearby_entities("w1", "e1")
    assert result == [{"id": "e2"}]


@respx.mock
async def test_get_location_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/locations/l1").mock(
        return_value=httpx.Response(200, json={"id": "l1", "name": "Düsterbruch"})
    )
    result = await client.get_location("l1")
    assert result == {"id": "l1", "name": "Düsterbruch"}


@respx.mock
async def test_get_region_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/worlds/w1/regions/r1").mock(
        return_value=httpx.Response(200, json={"id": "r1", "name": "Norden"})
    )
    result = await client.get_region("w1", "r1")
    assert result == {"id": "r1", "name": "Norden"}


@respx.mock
async def test_get_entity_events(client: BackendClient) -> None:
    respx.get(f"{BASE}/entity-events?entityType=npc&entityId=e1&limit=5").mock(
        return_value=httpx.Response(200, json=[{"id": 42, "event_type": "MOVED"}])
    )
    result = await client.get_entity_events("npc", "e1", 5)
    assert result == [{"id": 42, "event_type": "MOVED"}]


@respx.mock
async def test_get_weather_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/regions/r1/weather").mock(
        return_value=httpx.Response(200, json={"condition": "sunny", "temp": 22})
    )
    result = await client.get_weather("r1")
    assert result == {"condition": "sunny", "temp": 22}


@respx.mock
async def test_get_weather_error(client: BackendClient) -> None:
    respx.get(f"{BASE}/regions/r1/weather").mock(return_value=httpx.Response(500))
    result = await client.get_weather("r1")
    assert result is None


@respx.mock
async def test_add_memory_ok(client: BackendClient) -> None:
    route = respx.post(f"{BASE}/entities/e1/memories").mock(
        return_value=httpx.Response(200)
    )
    result = await client.add_memory("e1", "e2", "met", 1, "they met")
    assert result is True
    assert route.called


@respx.mock
async def test_add_memory_error(client: BackendClient) -> None:
    respx.post(f"{BASE}/entities/e1/memories").mock(return_value=httpx.Response(500))
    result = await client.add_memory("e1", "e2", "met", 1, "they met")
    assert result is False


@respx.mock
async def test_get_memories_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/entities/e1/memories").mock(
        return_value=httpx.Response(200, json=[{"id": "m1", "summary": "test"}])
    )
    result = await client.get_memories("e1")
    assert result == [{"id": "m1", "summary": "test"}]


@respx.mock
async def test_get_memories_error(client: BackendClient) -> None:
    respx.get(f"{BASE}/entities/e1/memories").mock(return_value=httpx.Response(500))
    result = await client.get_memories("e1")
    assert result == []


@respx.mock
async def test_get_bot_worlds_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/bot/worlds").mock(return_value=httpx.Response(
        200, json=[{"worldId": "w1", "campaigns": [{"id": "c1", "botMode": "off"}]}]))
    result = await client.get_bot_worlds()
    assert result[0]["campaigns"][0]["botMode"] == "off"


@respx.mock
async def test_get_bot_worlds_error_returns_empty(client: BackendClient) -> None:
    respx.get(f"{BASE}/bot/worlds").mock(return_value=httpx.Response(403))
    assert await client.get_bot_worlds() == []


@respx.mock
async def test_submit_intent_ok(client: BackendClient) -> None:
    route = respx.post(f"{BASE}/npc-intents").mock(
        return_value=httpx.Response(201, json={"id": "int1"})
    )
    result = await client.submit_intent(
        world_id="w1", npc_id="e1", intent_type="MOVE",
        params={"target_id": "l2"}, reasoning="zu gefährlich", campaign_id="c1",
    )
    assert result == {"id": "int1"}
    sent = json.loads(route.calls.last.request.content)
    assert sent == {
        "worldId": "w1", "npcId": "e1", "intentType": "MOVE",
        "paramsJson": {"target_id": "l2"}, "reasoning": "zu gefährlich",
        "campaignId": "c1",
    }


@respx.mock
async def test_submit_intent_error(client: BackendClient) -> None:
    respx.post(f"{BASE}/npc-intents").mock(return_value=httpx.Response(422))
    result = await client.submit_intent(
        world_id="w1", npc_id="e1", intent_type="ATTACK",
        params={}, reasoning="",
    )
    assert result is None


@respx.mock
async def test_get_faction_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/factions/f1").mock(
        return_value=httpx.Response(200, json={"id": "f1", "name": "Crown"})
    )
    result = await client.get_faction("f1")
    assert result == {"id": "f1", "name": "Crown"}


@respx.mock
async def test_get_faction_not_found(client: BackendClient) -> None:
    respx.get(f"{BASE}/factions/foo").mock(return_value=httpx.Response(404))
    result = await client.get_faction("foo")
    assert result is None


@respx.mock
async def test_get_faction_relations_ok(client: BackendClient) -> None:
    respx.get(f"{BASE}/factions/f1/relations").mock(
        return_value=httpx.Response(200, json=[
            {"id": "r1", "faction_a_id": "f1", "faction_b_id": "f2", "relation_status": "WAR"}
        ])
    )
    result = await client.get_faction_relations("f1")
    assert len(result) == 1
    assert result[0]["relation_status"] == "WAR"


@respx.mock
async def test_get_faction_relations_error(client: BackendClient) -> None:
    respx.get(f"{BASE}/factions/f1/relations").mock(return_value=httpx.Response(500))
    result = await client.get_faction_relations("f1")
    assert result == []
