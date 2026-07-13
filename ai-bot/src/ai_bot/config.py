from __future__ import annotations

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Backend
    backend_url: str = "http://localhost:8080/api/v1"
    service_token: str | None = None

    # LLM
    llm_type: str = "ollama"          # "ollama" | "vllm"
    llm_url: str = "http://localhost:8000"
    llm_model: str = "Qwen/Qwen2.5-7B-Instruct"
    ollama_url: str = "http://localhost:11434"
    ollama_model: str = "llama3"

    # Bot
    mode: str = "autonom"             # autonom | suggest | off
    poll_interval_ms: int = 2000
    context_max_tokens: int = 4096
    llm_timeout_s: int = 30

    model_config = {"env_prefix": "ai_bot_"}


settings = Settings()
