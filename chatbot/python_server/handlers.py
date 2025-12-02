from __future__ import annotations

import json
import uuid

from abstract_plan_service import AbstractPlanService
from detail_plan_service import DetailPlanService
from session_store import SessionStore


class ClientRequestHandler:
    def __init__(
        self,
        abstract_service: AbstractPlanService,
        detail_service: DetailPlanService,
        session_store: SessionStore,
    ) -> None:
        self.abstract_service = abstract_service
        self.detail_service = detail_service
        self.session_store = session_store

    def handle(self, conn, addr):
        print(f"[클라이언트 접속] {addr}")
        session_id = str(uuid.uuid4())
        buffer = ""
        try:
            while True:
                chunk = conn.recv(4096)
                if not chunk:
                    break
                buffer += chunk.decode("utf-8")
                while "\n" in buffer:
                    raw, buffer = buffer.split("\n", 1)
                    if not raw.strip():
                        continue
                    try:
                        message = json.loads(raw)
                    except json.JSONDecodeError:
                        self._send_error(conn, "잘못된 JSON 형식입니다.")
                        continue
                    try:
                        self._process_message(conn, session_id, message)
                    except Exception as exc:
                        print("[핸들러 오류]", exc)
                        self._send_error(conn, str(exc))
        finally:
            conn.close()
            print(f"[클라이언트 종료] {addr}")

    def _process_message(self, conn, session_id, message):
        msg_type = message.get("type")
        if msg_type == "plan_request":
            self._handle_plan_request(conn, session_id, message)
        elif msg_type == "plan_selection":
            self._handle_plan_selection(conn, session_id, message)
        elif msg_type == "day_detail_request":
            self._handle_day_detail(conn, message)
        else:
            self._send_error(conn, f"지원하지 않는 메시지 타입: {msg_type}")

    def _handle_plan_request(self, conn, session_id, message):
        print("플랜 요청 수신")
        candidates = self.abstract_service.generate_plan_candidates(message)
        self.session_store.save(session_id, candidates, message)
        plan_count = self.abstract_service.count_plan_entries(candidates)
        response = {
            "type": "plan_recommendations",
            "session_id": session_id,
            "plans": candidates,
        }
        if isinstance(candidates, dict):
            source = candidates.get("source")
            if source:
                response["source"] = source
        print(f"[추천 응답] source={response.get('source', 'n/a')} plans={plan_count}")
        self._send_json(conn, response)

    def _handle_plan_selection(self, conn, session_id, message):
        print("사용자 선택 수신")
        session_ref = message.get("session_id") or session_id
        if isinstance(session_ref, str):
            session_ref = session_ref.strip() or session_id
        cache = self.session_store.get(session_ref)
        if not cache:
            self._send_error(conn, "세션 정보가 없습니다. 다시 요청해주세요.")
            return

        selected_ids = self._extract_selection_ids(message)
        if not selected_ids:
            self._send_error(conn, "선택된 코스가 없습니다.")
            return

        selected_plan = self.abstract_service.extract_selected_plan(cache, selected_ids[0])
        if not selected_plan:
            self._send_error(conn, "유효하지 않은 코스 ID 입니다.")
            return

        user_request = str(message.get("user_request", "") or "").strip()
        if user_request:
            print(f"[사용자 추가 요청] {user_request}")
        else:
            print("[사용자 추가 요청] (입력 없음)")

        final_plan = self.detail_service.generate_final_plan(message, selected_plan)
        response = {"type": "plan_final", "final_plan": final_plan}
        if user_request:
            response["user_request"] = user_request
        self._send_json(conn, response)

    def _handle_day_detail(self, conn, message):
        session_ref = str(message.get("session_id", "")).strip()
        if not session_ref:
            self._send_error(conn, "세션 ID를 전달해주세요.")
            return
        cache = self.session_store.get(session_ref)
        if not cache:
            self._send_error(conn, "세션 정보가 없습니다. 다시 요청해주세요.")
            return
        try:
            day_value = int(message.get("day", 0))
        except (TypeError, ValueError):
            self._send_error(conn, "일차(day)는 숫자로 입력해주세요.")
            return
        if day_value <= 0:
            self._send_error(conn, "1 이상의 일차를 입력해주세요.")
            return
        day_entry = self.abstract_service.find_day_entry(cache.get("plans"), day_value)
        if not day_entry:
            self._send_error(conn, f"{day_value}일차 플랜을 찾을 수 없습니다.")
            return
        summaries = []
        for plan in day_entry.get("plans", []):
            summaries.append(
                {
                    "plan_id": plan.get("id"),
                    "title": plan.get("title"),
                    "activities": self.abstract_service.summarize_plan(plan),
                }
            )
        self._send_json(
            conn,
            {
                "type": "day_detail",
                "session_id": session_ref,
                "day": day_value,
                "plans": summaries,
            },
        )

    def _extract_selection_ids(self, message: dict) -> list[str]:
        if "selected_plan_id" in message:
            return [message["selected_plan_id"]]
        plans = message.get("selected_plans")
        if isinstance(plans, list):
            return [str(plan).strip() for plan in plans if plan]
        return []

    def _send_json(self, conn, payload):
        conn.sendall((json.dumps(payload, ensure_ascii=False) + "\n").encode("utf-8"))

    def _send_error(self, conn, message):
        self._send_json(conn, {"type": "error", "message": message})

