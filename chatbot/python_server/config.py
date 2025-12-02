from __future__ import annotations

import os


def load_required_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"{name} 환경 변수가 설정되어 있지 않습니다.")
    return value


class ServerConfig:
    def __init__(self) -> None:
        self.chatbot_api_key = load_required_env("CHATBOT_AI_API_KEY")
        self.openai_timeout = float(os.getenv("OPENAI_TIMEOUT_SECONDS", "30"))
        self.openai_max_retries = int(os.getenv("OPENAI_MAX_RETRIES", "0"))
        self.server_host = os.getenv("CHATBOT_SERVER_HOST", "0.0.0.0")
        self.server_port = int(os.getenv("CHATBOT_SERVER_PORT", "7777"))

