from __future__ import annotations

import random


FALLBACK_TIME_SLOTS = ["09:00", "11:30", "14:00", "16:30", "19:00"]
FALLBACK_THEME_ACTIVITIES = {
    "자연": [
        "숲길 트레킹",
        "전망대에서 일출 감상",
        "해변 산책",
        "계곡 피크닉",
        "국립공원 탐방",
        "온천 휴식",
    ],
    "역사": [
        "박물관 관람",
        "문화재 해설 투어",
        "전통 한옥 마을 산책",
        "사찰 예불 체험",
        "근대사 거리 투어",
    ],
    "체험": [
        "공방 체험 클래스",
        "지역 축제 참가",
        "전통 음식 만들기",
        "야외 액티비티 체험",
        "야시장 미션 투어",
    ],
    "레저": [
        "서핑/패들보드 강습",
        "수상 레포츠",
        "자전거 하이킹",
        "골프/스크린골프",
        "암벽 클라이밍",
    ],
    "쇼핑": [
        "명동 패션 쇼핑",
        "프리미엄 아울렛 방문",
        "현지 재래시장 구경",
        "디자인 편집숍 탐방",
        "기념품 골라보기",
    ],
    "음식": [
        "미쉐린 맛집 투어",
        "재래시장 먹거리 탐방",
        "지역 카페 라운지",
        "야식 골목 탐험",
        "수제주/와인 시음",
    ],
    "숙박": [
        "부티크 호텔 투어",
        "오션뷰 수영장 휴식",
        "야경이 보이는 라운지",
        "미니 스파 타임",
        "룸서비스 디너",
    ],
    "기타": [
        "인기 촬영지 방문",
        "강변 산책",
        "전망대 야경 촬영",
        "도심 예술 산책",
        "현지 북카페 휴식",
    ],
    "default": [
        "시그니처 랜드마크 방문",
        "현지 맛집 점심",
        "전통시장 구경",
        "뮤지엄 또는 갤러리",
        "야경 명소 감상",
    ],
}

FALLBACK_INDOOR_ACTIVITIES = [
    "현대미술관 투어",
    "프리미엄 카페 라운지 휴식",
    "전통 공예 클래스",
    "실내 전망 라운지에서 도시 야경 감상",
    "한식 쿠킹 클래스",
    "도심 온천/스파 휴식",
    "아쿠아리움 탑승형 체험",
    "VR/미디어 아트 체험관",
    "실내 복합 문화공간 산책",
    "뮤지컬 또는 공연 관람",
]

FALLBACK_REQUEST_ACTIVITIES = {
    "kids": [
        "어린이 박물관 체험",
        "가족 체험 공방",
        "아쿠아리움 투어",
        "키즈 카페 휴식",
        "과학관 상설전 관람",
    ],
    "food": [
        "지역 맛집 투어",
        "디저트 플레이트 카페",
        "전통 시장 먹거리 탐방",
        "현지 쿠킹 클래스",
        "야시장 미식 산책",
    ],
    "relax": [
        "호텔 라운지 티타임",
        "스파와 사우나 휴식",
        "온천에서 피로 풀기",
        "루프탑 카페 한적한 시간",
        "숲속 힐링 센터 명상",
    ],
    "photo": [
        "야경 포토 스팟 투어",
        "전망대 파노라마 촬영",
        "감성 스튜디오 촬영",
        "벽화 거리 포토 워크",
        "꽃·식물원 인생샷 투어",
    ],
}


def analyze_user_request(user_request: str | None) -> dict:
    context = {
        "prefer_indoor": False,
        "prefer_kids": False,
        "prefer_food": False,
        "prefer_relax": False,
        "prefer_photo": False,
    }
    if not user_request:
        return context

    lowered = str(user_request).lower()

    def _contains(words):
        return any(word in lowered for word in words)

    context["prefer_indoor"] = _contains(
        ("날씨", "실내", "비", "우천", "우산", "눈", "폭설", "폭우", "rain", "snow", "indoor")
    )
    context["prefer_kids"] = _contains(("아이", "키즈", "어린이", "family", "kid", "연인"))
    context["prefer_food"] = _contains(("맛집", "식사", "먹거리", "food", "restaurant", "미식"))
    context["prefer_relax"] = _contains(("휴식", "스파", "힐링", "relax", "온천", "휴양"))
    context["prefer_photo"] = _contains(("사진", "포토", "인생샷", "야경", "촬영", "photo"))
    return context


def needs_additional_suggestions(user_request: str | None) -> bool:
    if not user_request:
        return False
    lowered = str(user_request).lower()
    keywords = (
        "추가",
        "더",
        "다른 일정",
        "extra",
        "another",
        "more",
        "add",
        "spare",
        "보충",
        "서브",
        "서브 일정",
    )
    return any(keyword in lowered for keyword in keywords)


def collect_theme_keys(themes):
    if not themes:
        return ["default"]
    keys = []
    for theme in themes:
        if not theme:
            continue
        text = str(theme)
        for keyword in FALLBACK_THEME_ACTIVITIES.keys():
            if keyword == "default":
                continue
            if keyword in text and keyword not in keys:
                keys.append(keyword)
    if "default" not in keys:
        keys.append("default")
    return keys


def build_activity_pool(themes, force_indoor=False, context=None):
    if force_indoor:
        pool = set(FALLBACK_INDOOR_ACTIVITIES)
    else:
        keys = collect_theme_keys(themes)
        pool = set()
        for key in keys:
            pool.update(FALLBACK_THEME_ACTIVITIES.get(key, []))
        if not pool:
            pool.update(FALLBACK_THEME_ACTIVITIES["default"])

    if context:
        if context.get("prefer_kids"):
            pool.update(FALLBACK_REQUEST_ACTIVITIES["kids"])
        if context.get("prefer_food"):
            pool.update(FALLBACK_REQUEST_ACTIVITIES["food"])
        if context.get("prefer_relax"):
            pool.update(FALLBACK_REQUEST_ACTIVITIES["relax"])
        if context.get("prefer_photo"):
            pool.update(FALLBACK_REQUEST_ACTIVITIES["photo"])

    return list(pool)


def build_fallback_schedule(
    destination,
    themes,
    used_activity_signatures=None,
    *,
    force_indoor=False,
    context=None,
):
    activity_pool = build_activity_pool(themes, force_indoor=force_indoor, context=context)
    random.shuffle(activity_pool)
    slot_count = min(len(activity_pool), len(FALLBACK_TIME_SLOTS))
    slot_count = max(2, slot_count)
    selected = []
    used_activity_signatures = used_activity_signatures or set()
    for activity in activity_pool:
        signature = f"{destination} {activity}"
        if signature in used_activity_signatures:
            continue
        used_activity_signatures.add(signature)
        selected.append(signature)
        if len(selected) == slot_count:
            break
    if len(selected) < slot_count:
        remaining = [
            f"{destination} {act}"
            for act in activity_pool
            if f"{destination} {act}" not in selected
        ]
        while len(selected) < slot_count and remaining:
            pick = remaining.pop(0)
            selected.append(pick)
    times = random.sample(FALLBACK_TIME_SLOTS, slot_count)
    schedule = []
    for time, activity in zip(times, selected):
        schedule.append({"time": time, "activity": activity})
    return schedule


def build_candidate_days(payload):
    destination = payload.get("destination", "추천 여행지")
    days = max(1, int(payload.get("days", 1)))
    themes = payload.get("theme") or []
    day_entries = []
    for index in range(days):
        day_label = f"Day {index + 1}"
        plans = []
        day_used_signatures = set()
        for plan_code in ("A", "B", "C"):
            plan_id = f"{index + 1}-{plan_code}"
            schedule = build_fallback_schedule(destination, themes, day_used_signatures)
            plans.append(
                {
                    "id": plan_id,
                    "title": f"{destination} {day_label} 플랜 {plan_code}",
                    "schedule": schedule,
                }
            )
        day_entries.append({"day": index + 1, "title": day_label, "plans": plans})
    return {"source": "fallback", "days": day_entries}


def build_additional_suggestions(
    payload,
    context,
    count=2,
):
    destination = payload.get("destination", "추천 여행지")
    themes = payload.get("theme") or []
    context = context or {}
    force_indoor = context.get("prefer_indoor")
    used_signatures = set()
    suggestions = []
    for idx in range(count):
        schedule = build_fallback_schedule(
            destination,
            themes,
            used_signatures,
            force_indoor=bool(force_indoor),
            context=context,
        )
        suggestions.append(
            {
                "title": f"추가 제안 {idx + 1}",
                "schedule": schedule,
            }
        )
    return suggestions


def build_fallback_final_plan(payload, selected_plan):
    destination = payload.get("destination", "추천 여행지")
    themes = payload.get("theme") or []
    user_request = str(payload.get("user_request", "") or "").strip()
    request_context = analyze_user_request(user_request)
    prioritize_indoor = request_context.get("prefer_indoor")
    request_context["prefer_indoor"] = bool(prioritize_indoor)
    needs_custom_schedule = any(
        (
            request_context.get("prefer_indoor"),
            request_context.get("prefer_kids"),
            request_context.get("prefer_food"),
            request_context.get("prefer_relax"),
            request_context.get("prefer_photo"),
        )
    )
    schedule = selected_plan.get("schedule")
    if needs_custom_schedule or not schedule:
        schedule = build_fallback_schedule(
            destination,
            themes,
            force_indoor=bool(prioritize_indoor),
            context=request_context,
        )
    summary = f"{destination} 기본 일정"
    if request_context.get("prefer_indoor"):
        summary += " · 실내 위주 대체"
    elif request_context.get("prefer_kids"):
        summary += " · 가족 친화 코스"
    elif request_context.get("prefer_relax"):
        summary += " · 휴식 중심"
    plan = {
        "summary": summary,
        "source": "fallback",
        "days": [
            {
                "title": "Day 1",
                "schedule": schedule,
            }
        ],
    }
    if user_request:
        plan["notes"] = user_request
    if any(request_context.values()):
        plan["request_context"] = request_context
    if needs_additional_suggestions(user_request):
        extras = build_additional_suggestions(payload, request_context)
        if extras:
            plan["additional_recommendations"] = extras
    return plan


def count_plan_entries(node):
    if node is None:
        return 0
    count = 0
    if isinstance(node, dict):
        plans = node.get("plans")
        if isinstance(plans, list):
            for item in plans:
                if isinstance(item, dict):
                    if item.get("schedule") or item.get("activities"):
                        count += 1
        days = node.get("days")
        if isinstance(days, list):
            for day in days:
                count += count_plan_entries(day)
        options = node.get("options")
        if isinstance(options, list):
            for option in options:
                count += count_plan_entries(option)
        variants = node.get("variants")
        if isinstance(variants, list):
            for variant in variants:
                count += count_plan_entries(variant)
    elif isinstance(node, list):
        for item in node:
            count += count_plan_entries(item)
    return count


def find_day_entry(plans_data, target_day):
    if not plans_data:
        return None
    for entry in plans_data.get("days", []):
        number = normalize_day_number(entry.get("day"), entry.get("title", ""))
        if number == target_day:
            return entry
    return None


def extract_selected_plan(cache, selected_id):
    candidates = cache.get("plans", {})
    for day in candidates.get("days", []):
        for plan in day.get("plans", []):
            if plan.get("id") == selected_id:
                return plan
    return None


def summarize_schedule(plan):
    activities = []
    schedule = plan.get("schedule", [])
    if isinstance(schedule, list):
        for item in schedule:
            if isinstance(item, dict):
                time = str(item.get("time", "")).strip()
                activity = str(item.get("activity", "")).strip()
                parts = [value for value in (time, activity) if value]
                if parts:
                    activities.append(" ".join(parts))
            elif item:
                activities.append(str(item))
    elif schedule:
        activities.append(str(schedule))
    return activities


def normalize_day_number(day_value, title=""):
    if isinstance(day_value, (int, float)):
        return max(0, int(day_value))
    candidate = None
    if day_value is not None:
        candidate = str(day_value)
    elif title:
        candidate = title
    if candidate:
        digits = "".join(ch for ch in candidate if ch.isdigit())
        if digits:
            try:
                return int(digits)
            except ValueError:
                pass
    return 0

