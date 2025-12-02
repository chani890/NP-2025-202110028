from __future__ import annotations

import json

from ai_plan_client import AIPlanClient
import fallback_utils as fallback


class AbstractPlanService:
    def __init__(self, ai_client: AIPlanClient) -> None:
        self.ai_client = ai_client

    def generate_plan_candidates(self, payload: dict) -> dict:
        messages = self._make_candidate_prompt(payload)
        try:
            response = self.ai_client.create_completion(messages)
            text = AIPlanClient.extract_message_text(response.choices[0])
            if not text:
                raise ValueError("모델 응답이 비어 있습니다.")
            parsed = json.loads(text)
            if fallback.count_plan_entries(parsed) == 0:
                raise ValueError("모델 응답에 플랜이 없습니다.")
            return parsed
        except Exception as exc:
            print("[OpenAI 플랜 추천 오류]", exc)
            return fallback.build_candidate_days(payload)

    @staticmethod
    def count_plan_entries(node):
        return fallback.count_plan_entries(node)

    @staticmethod
    def find_day_entry(plans_data, target_day):
        return fallback.find_day_entry(plans_data, target_day)

    @staticmethod
    def extract_selected_plan(cache, selected_id):
        return fallback.extract_selected_plan(cache, selected_id)

    @staticmethod
    def summarize_plan(plan):
        return fallback.summarize_schedule(plan)

    def _make_candidate_prompt(self, payload: dict) -> list:
        system_prompt = (
            "당신은 여행 일정 추천 전문가입니다."
            "사용자가 입력한 여행 조건(여행지, 여행 기간, 테마, 연령 등)을 기반으로,"
            "각 날짜별로 2~3개의 대체 코스(Plan A, Plan B, Plan C)를 생성해야 합니다."
            "요구사항:"
            "1) 각 날짜는 Plan A, Plan B, Plan C로 구성합니다."
            "2) 각 코스는 3~6개의 활동으로 구성합니다."
            "3) 활동은 time(시간) + activity(활동) 구조로 작성합니다."
            "4) 이동 동선이 자연스럽고 실제 가능한 일정으로 구성해야 합니다."
            "6) 반드시 JSON 형식으로만 출력합니다."
            "7) Day1, Day2 … 형태로 배열 days 안에 구조화합니다."
        )
        user_content = {
            "destination": payload["destination"],
            "days": payload["days"],
            "themes": payload["theme"],
            "age": payload["age"],
        }
        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": json.dumps(user_content, ensure_ascii=False)},
        ]

