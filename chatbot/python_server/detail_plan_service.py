from __future__ import annotations

import json

from ai_plan_client import AIPlanClient
import fallback_utils as fallback


class DetailPlanService:
    def __init__(self, ai_client: AIPlanClient) -> None:
        self.ai_client = ai_client

    def generate_final_plan(self, payload: dict, selected_plan: dict) -> dict:
        messages = self._make_final_prompt(payload, selected_plan)
        try:
            response = self.ai_client.create_completion(messages)
            text = AIPlanClient.extract_message_text(response.choices[0])
            if not text:
                raise ValueError("모델 응답이 비어 있습니다.")
            parsed = json.loads(text)
            if not fallback.count_plan_entries(parsed):
                raise ValueError("모델 응답에 유효한 세부 일정이 없습니다.")
            user_request = str(payload.get("user_request", "") or "").strip()
            if fallback.needs_additional_suggestions(user_request):
                context = fallback.analyze_user_request(user_request)
                extras = fallback.build_additional_suggestions(payload, context)
                if extras:
                    parsed["additional_recommendations"] = extras
            return parsed
        except Exception as exc:
            print("[OpenAI 최종 플랜 오류]", exc)
            return fallback.build_fallback_final_plan(payload, selected_plan)

    def _make_final_prompt(self, preference: dict, selected_plan: dict) -> list:
        system_prompt = (
            "선택된 코스를 기반으로 최종 여행 일정을 JSON으로 구성하세요."
            "가능하다면 사용자의 추가 요청사항(user_request)을 반영해 세부 일정을 조정하세요."
        )
        user_content = {
            "destination": preference["destination"],
            "selected_plan": selected_plan,
        }
        request_text = preference.get("user_request")
        if request_text:
            user_content["user_request"] = request_text
        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": json.dumps(user_content, ensure_ascii=False)},
        ]

