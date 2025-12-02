from __future__ import annotations

import threading


class SessionStore:
    def __init__(self) -> None:
        self._sessions: dict[str, dict] = {}
        self._lock = threading.Lock()

    def save(self, session_id: str, plans: dict, request: dict) -> None:
        with self._lock:
            self._sessions[session_id] = {"plans": plans, "request": request}

    def get(self, session_id: str) -> dict | None:
        with self._lock:
            return self._sessions.get(session_id)

