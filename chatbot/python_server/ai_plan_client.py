from __future__ import annotations

from openai import OpenAI

from config import ServerConfig


class AIPlanClient:
    def __init__(self, config: ServerConfig) -> None:
        self._client = OpenAI(api_key=config.chatbot_api_key)
        self._timeout = config.openai_timeout
        self._max_retries = config.openai_max_retries

    def create_completion(self, messages):
        configured = self._client.with_options(
            timeout=self._timeout,
            max_retries=self._max_retries,
        )
        return configured.chat.completions.create(
            model="gpt-4.1-mini",
            messages=messages,
            temperature=0.7,
        )

    @staticmethod
    def extract_message_text(choice):
        message = getattr(choice, "message", choice)
        content = getattr(message, "content", "")
        if isinstance(content, str):
            return content.strip()
        if isinstance(content, (list, tuple)):
            parts = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                    continue
                text = getattr(item, "text", None)
                if text:
                    parts.append(text)
                elif isinstance(item, dict):
                    text = item.get("text")
                    if text:
                        parts.append(text)
            return "\n".join(parts).strip()
        if content is None:
            return ""
        return str(content).strip()

