"""
FastAPI-App für den LWE AI-Bot.
Liest Config aus Umgebungsvariablen (AI_BOT_*).
"""
from __future__ import annotations

import asyncio
import logging

from fastapi import FastAPI

from src.ai_bot.config import settings
from src.ai_bot.poller import EventPoller

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("ai_bot")

app = FastAPI(title="LWE AI Bot", version="0.1.0")
poller = EventPoller()


@app.on_event("startup")
async def startup():
    logger.info("Starting AI Bot (mode=%s, llm=%s, model=%s)",
                 settings.mode, settings.llm_type,
                 settings.ollama_model if settings.llm_type == "ollama" else settings.llm_model)
    asyncio.create_task(poller.run())


@app.get("/health")
async def health():
    return {"status": "UP", "mode": settings.mode, "llm": settings.llm_type}
