"""
FastAPI-App für den LWE AI-Bot.
Liest Config aus Umgebungsvariablen (AI_BOT_*).
"""
from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from ai_bot.config import settings
from ai_bot.poller import EventPoller

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("ai_bot")

poller = EventPoller()
_poller_task: asyncio.Task[None] | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _poller_task
    logger.info("Starting AI Bot (mode=%s, llm=%s, model=%s)",
                 settings.mode, settings.llm_type,
                 settings.ollama_model if settings.llm_type == "ollama" else settings.llm_model)
    _poller_task = asyncio.create_task(poller.run())
    yield
    if _poller_task is not None:
        _poller_task.cancel()
        try:
            await _poller_task
        except asyncio.CancelledError:
            pass
        _poller_task = None


app = FastAPI(title="LWE AI Bot", version="0.1.0", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "UP", "mode": settings.mode, "llm": settings.llm_type}
