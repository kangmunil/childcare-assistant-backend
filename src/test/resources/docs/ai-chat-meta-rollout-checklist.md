# AI Chat Meta Rollout Checklist

## Feature Flags
- `AI_CHAT_META_ENABLED` (Spring: `ai.chat-meta.enabled`)
- `AI_CHAT_META_ROLLOUT_PERCENT` (Spring: `ai.chat-meta.rollout-percent`)
- `AI_CHAT_META_ENABLED` (FastAPI env, defaults true)
- `VITE_AI_CHAT_META_UI_ENABLED` (frontend env)
- `VITE_AI_CHAT_FEEDBACK_ENABLED` (frontend env)

## Structured Log Events
- `AI_CHAT_REQUEST`
  - keys: `request_id`, `member_id_hash`, `session_id`, `child_id_present`, `intent`, `requested_domains`, `context_mode`, `has_manual_context`, `message_length`
- `AI_CHAT_RESPONSE`
  - keys: `request_id`, `session_id`, `member_id_hash`, `response_mode`, `intent`, `elapsed_ms`, `fallback_code`, `citation_count`, `quick_action_count`, `confidence_bucket`, `meta_exposed`
- `AI_CHAT_FEEDBACK`
  - keys: `request_id`, `session_id`, `member_id_hash`, `rating`, `reason_code`, `response_mode`, `intent`, `is_first_ai_answer`

## Suggested Operational Queries
- Fallback ratio (daily): `response_mode=FALLBACK / total responses`
- Clarify ratio (daily): `response_mode=CLARIFY / total responses`
- Positive feedback ratio: `rating=UP / total feedback`
- Top downvote reasons: group by `reason_code` where `rating=DOWN`
- Meta exposure ratio: `meta_exposed=true / total responses`

## Manual QA Scenarios
1. 성장 질문 + 자녀 미선택 → `CLARIFY` response with quick action
2. 지역 없는 "어린이집 찾아줘" → location clarify
3. AI upstream failure → `FALLBACK` response with retry action
4. 성장/프로필 기반 답변 → citation label rendered
5. 👍/👎 feedback submission → `AI_CHAT_FEEDBACK` log emitted
