from __future__ import annotations

from fastapi.testclient import TestClient
from src.ai_bot.main import app

client = TestClient(app)


def test_health() -> None:
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "UP"
    assert "mode" in data
    assert "llm" in data
