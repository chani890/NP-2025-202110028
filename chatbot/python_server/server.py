from __future__ import annotations

from abstract_plan_service import AbstractPlanService
from ai_plan_client import AIPlanClient
from config import ServerConfig
from detail_plan_service import DetailPlanService
from handlers import ClientRequestHandler
from session_store import SessionStore
from tcp_server import ChatbotTCPServer


def main() -> None:
    config = ServerConfig()
    ai_client = AIPlanClient(config)
    abstract_service = AbstractPlanService(ai_client)
    detail_service = DetailPlanService(ai_client)
    session_store = SessionStore()

    handler_factory = lambda: ClientRequestHandler(
        abstract_service,
        detail_service,
        session_store,
    )
    server = ChatbotTCPServer(config.server_host, config.server_port, handler_factory)
    server.start()


if __name__ == "__main__":
    main()
