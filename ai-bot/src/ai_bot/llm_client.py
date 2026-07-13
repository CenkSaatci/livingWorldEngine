"""
LLM-Client-Interface mit zwei Implementierungen:
- Ollama (eigene JSON-API)
- vLLM (OpenAI-kompatibel, /v1/chat/completions)
"""
from __future__ import annotations

import json
from abc import ABC, abstractmethod

import httpx
from src.ai_bot.config import settings


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


def create_llm_client() -> LLMClient:
    if settings.llm_type == "vllm":
        return VLLMClient()
    return OllamaClient()
