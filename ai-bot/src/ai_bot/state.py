"""Persistenz für Poller-Offset (last_ids pro Welt) via stdlib sqlite3.

Nur DURCHGEHENDE Offsets werden gespeichert (at-least-once); das Backend
dedupliziert Intents über event_hash (AI-AGENT.md §3.3).
"""
from __future__ import annotations

import sqlite3
from pathlib import Path


class StateStore:
    """Speichert world_id -> last_id. Thread-unsafe, aber der Poller ist single-task."""

    def __init__(self, path: str | Path) -> None:
        self._conn = sqlite3.connect(str(path))
        self._conn.execute(
            "CREATE TABLE IF NOT EXISTS last_ids (world_id TEXT PRIMARY KEY, last_id INTEGER NOT NULL)"
        )
        self._conn.commit()

    def load(self) -> dict[str, int]:
        return {w: i for w, i in self._conn.execute("SELECT world_id, last_id FROM last_ids")}

    def save(self, last_ids: dict[str, int]) -> None:
        self._conn.executemany(
            "INSERT INTO last_ids(world_id, last_id) VALUES(?, ?) "
            "ON CONFLICT(world_id) DO UPDATE SET last_id = excluded.last_id",
            list(last_ids.items()),
        )
        self._conn.commit()

    def close(self) -> None:
        self._conn.close()
