from __future__ import annotations

import logging
from typing import Literal

from pydantic import Field
from pydantic_settings import BaseSettings

logger = logging.getLogger("ai_bot.config")


class Settings(BaseSettings):
    # Backend
    backend_url: str = "http://localhost:8080/api/v1"
    service_token: str | None = None

    # LLM
    llm_type: Literal["ollama", "vllm", "openai"] = "ollama"  # "openai" = OpenAI-kompatibel (vLLM u.a.)
    llm_url: str = "http://localhost:11434"  # nicht :8000 — das ist der Bot-Port (bzw. vLLM im Compose-Netz)
    llm_model: str = "Qwen/Qwen2.5-7B-Instruct"
    ollama_url: str = "http://localhost:11434"
    ollama_model: str = "llama3"

    # Bot
    mode: Literal["autonom", "suggest", "off"] = "autonom"
    poll_interval_ms: int = Field(default=2000, gt=0)
    context_max_tokens: int = Field(default=4096, gt=0)
    llm_timeout_s: int = Field(default=30, gt=0)
    # SQLite-Datei für last_ids-Persistenz (AI-AGENT.md §3.3); None = in-memory (at-least-once via Backend-dedup)
    state_path: str | None = None

    model_config = {"env_prefix": "ai_bot_"}


settings = Settings()

if not settings.service_token:
    logger.warning("AI_BOT_SERVICE_TOKEN not set — backend calls run unauthenticated")
