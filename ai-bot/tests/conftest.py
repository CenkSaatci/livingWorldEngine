from __future__ import annotations

import pytest

from ai_bot.api_client import BackendClient
from ai_bot.llm_client import create_llm_client, LLMClient, OllamaClient, VLLMClient


@pytest.fixture
def backend_client() -> BackendClient:
    return BackendClient()


@pytest.fixture
def ollama_llm() -> LLMClient:
    return OllamaClient()


@pytest.fixture
def vllm_llm() -> LLMClient:
    return VLLMClient()


@pytest.fixture
def any_llm() -> LLMClient:
    return create_llm_client()
