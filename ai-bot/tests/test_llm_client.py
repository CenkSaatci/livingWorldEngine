from __future__ import annotations

import httpx
import pytest
import respx

from src.ai_bot.llm_client import OllamaClient, VLLMClient

OLLAMA_URL = "http://test-ollama:11434/api/generate"
VLLM_URL = "http://test-vllm:8000/v1/chat/completions"


@pytest.fixture
def ollama(monkeypatch: pytest.MonkeyPatch) -> OllamaClient:
    monkeypatch.setenv("AI_BOT_OLLAMA_URL", "http://test-ollama:11434")
    from src.ai_bot.config import settings
    settings.ollama_url = "http://test-ollama:11434"
    return OllamaClient()


@pytest.fixture
def vllm(monkeypatch: pytest.MonkeyPatch) -> VLLMClient:
    monkeypatch.setenv("AI_BOT_LLM_URL", "http://test-vllm:8000")
    from src.ai_bot.config import settings
    settings.llm_url = "http://test-vllm:8000"
    return VLLMClient()


@respx.mock
async def test_ollama_generate_ok(ollama: OllamaClient) -> None:
    respx.post(OLLAMA_URL).mock(
        return_value=httpx.Response(200, json={"response": '{"action": "MOVE", "target_id": "l2", "reasoning": "patrol"}'})
    )
    result = await ollama.generate("sys", "usr")
    assert result == {"action": "MOVE", "target_id": "l2", "reasoning": "patrol"}


@respx.mock
async def test_ollama_generate_invalid_json(ollama: OllamaClient) -> None:
    respx.post(OLLAMA_URL).mock(
        return_value=httpx.Response(200, json={"response": "not json at all"})
    )
    with pytest.raises(Exception):
        await ollama.generate("sys", "usr")


@respx.mock
async def test_ollama_http_error(ollama: OllamaClient) -> None:
    respx.post(OLLAMA_URL).mock(return_value=httpx.Response(500))
    with pytest.raises(httpx.HTTPStatusError):
        await ollama.generate("sys", "usr")


@respx.mock
async def test_vllm_generate_ok(vllm: VLLMClient) -> None:
    respx.post(VLLM_URL).mock(
        return_value=httpx.Response(200, json={
            "choices": [{"message": {"content": '{"action": "ATTACK", "target_id": null, "reasoning": "feindlich"}'}}]
        })
    )
    result = await vllm.generate("sys", "usr")
    assert result == {"action": "ATTACK", "target_id": None, "reasoning": "feindlich"}


@respx.mock
async def test_vllm_generate_invalid_json(vllm: VLLMClient) -> None:
    respx.post(VLLM_URL).mock(
        return_value=httpx.Response(200, json={
            "choices": [{"message": {"content": "not json"}}]
        })
    )
    with pytest.raises(Exception):
        await vllm.generate("sys", "usr")


@respx.mock
async def test_vllm_http_error(vllm: VLLMClient) -> None:
    respx.post(VLLM_URL).mock(return_value=httpx.Response(503))
    with pytest.raises(httpx.HTTPStatusError):
        await vllm.generate("sys", "usr")
