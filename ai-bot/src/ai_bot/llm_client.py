"""
LLM-Client-Interface mit zwei Implementierungen:
- Ollama (eigene JSON-API)
- vLLM (OpenAI-kompatibel, /v1/chat/completions)
"""
from __future__ import annotations

import json
from abc import ABC, abstractmethod

import httpx
from ai_bot.config import settings


class LLMClient(ABC):
    """Abstrakter LLM-Client. Antwort muss als JSON geparst werden können."""

    @abstractmethod
    async def generate(self, system_prompt: str, user_prompt: str) -> dict: ...


class OllamaClient(LLMClient):
    def __init__(self) -> None:
        self.url = f"{settings.ollama_url}/api/generate"
        self.model = settings.ollama_model
        self.timeout = settings.llm_timeout_s

    async def generate(self, system_prompt: str, user_prompt: str) -> dict:
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            resp = await client.post(self.url, json={
                "model": self.model,
                "system": system_prompt,
                "prompt": user_prompt,
                "stream": False,
                "format": "json",
            })
            resp.raise_for_status()
            return json.loads(resp.json()["response"])


class VLLMClient(LLMClient):
    """OpenAI-kompatibler Client für vLLM, TGI, OpenRouter, etc."""

    def __init__(self) -> None:
        self.url = f"{settings.llm_url}/v1/chat/completions"
        self.model = settings.llm_model
        self.timeout = settings.llm_timeout_s

    async def generate(self, system_prompt: str, user_prompt: str) -> dict:
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            resp = await client.post(self.url, json={
                "model": self.model,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                "temperature": 0.7,
                "max_tokens": 512,
                "response_format": {"type": "json_object"},
            })
            resp.raise_for_status()
            data = resp.json()
            content = data["choices"][0]["message"]["content"]
            return json.loads(content)


class MockLLMClient(LLMClient):
    """Test-Double (TESTING.md §4.1): liefert Canned-Responses, kein Netzwerk.

    Usage: MockLLMClient({"action": "IDLE", "reasoning": "test"})
    oder MockLLMClient([resp1, resp2]) für sequenzielle Antworten.
    """

    def __init__(self, responses: dict | list[dict] | None = None) -> None:
        if responses is None:
            responses = {"action": "IDLE", "reasoning": "mock"}
        self._queue: list[dict] = list(responses) if isinstance(responses, list) else [responses]

    async def generate(self, system_prompt: str, user_prompt: str) -> dict:
        if len(self._queue) > 1:
            return self._queue.pop(0)
        return self._queue[0]


FakeLLMClient = MockLLMClient


def create_llm_client(llm_type: str | None = None) -> LLMClient:
    t = (llm_type or settings.llm_type).lower()
    if t in ("vllm", "openai"):
        return VLLMClient()
    if t == "ollama":
        return OllamaClient()
    raise ValueError(f"Unknown LLM type: {t!r} (expected 'ollama', 'vllm' or 'openai')")
