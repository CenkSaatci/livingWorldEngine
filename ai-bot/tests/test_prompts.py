"""Golden-File-Prompt-Tests (TESTING.md §4.2, AI-AGENT.md §10).

Rendert die echten Jinja-Templates mit Fixture-Kontext und vergleicht
gegen tests/prompts/expected/*.txt — verhindert Prompt-Drift.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest

from ai_bot.poller import _jinja

FIXTURES = Path(__file__).parent / "fixtures" / "npc_contexts"
EXPECTED = Path(__file__).parent / "prompts" / "expected"

PERSONALITIES = ["aggressiv", "neutral", "vorsichtig"]


@pytest.mark.parametrize("personality", PERSONALITIES)
def test_golden_prompt(personality: str) -> None:
    ctx = json.loads((FIXTURES / f"{personality}.json").read_text(encoding="utf-8"))
    rendered = _jinja.get_template(f"{personality}.j2").render(**ctx)
    golden = (EXPECTED / f"{personality}.txt").read_text(encoding="utf-8")
    assert rendered == golden
