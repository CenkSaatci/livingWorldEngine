"""TDD (CRITICAL 5): Compose-/.env-Namen müssen an Settings binden.

Regressionsschutz: Die alten, ins Leere zeigenden Namen (LWE_API_URL,
LWE_API_KEY, AI_BOT_POLL_INTERVAL_S, AI_LLM_*) dürfen NICHT binden —
nur AI_BOT_BACKEND_URL / AI_BOT_SERVICE_TOKEN / AI_BOT_POLL_INTERVAL_MS.
"""
from __future__ import annotations

import pytest
from pydantic import ValidationError

from ai_bot.config import Settings


def test_ai_bot_prefixed_vars_bind(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_BOT_BACKEND_URL", "https://dein-server.com/api/v1")
    monkeypatch.setenv("AI_BOT_SERVICE_TOKEN", "tok123")
    monkeypatch.setenv("AI_BOT_POLL_INTERVAL_MS", "5000")
    s = Settings()
    assert s.backend_url == "https://dein-server.com/api/v1"
    assert s.service_token == "tok123"
    assert s.poll_interval_ms == 5000


def test_legacy_names_do_not_bind(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("AI_BOT_BACKEND_URL", raising=False)
    monkeypatch.delenv("AI_BOT_SERVICE_TOKEN", raising=False)
    monkeypatch.delenv("AI_BOT_POLL_INTERVAL_MS", raising=False)
    monkeypatch.setenv("LWE_API_URL", "https://wrong/api/v1")
    monkeypatch.setenv("LWE_API_KEY", "wrong")
    monkeypatch.setenv("AI_BOT_POLL_INTERVAL_S", "99")
    s = Settings()
    assert s.backend_url != "https://wrong/api/v1"
    assert s.service_token != "wrong"
    assert s.poll_interval_ms != 99000


def test_invalid_llm_type_rejected(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_BOT_LLM_TYPE", "gpt-neo-x")
    with pytest.raises(ValidationError):
        Settings()
