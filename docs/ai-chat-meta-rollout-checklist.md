# AI Chat Meta Rollout Checklist

## 1) Feature Flags
- Frontend
  - `VITE_AI_CHAT_META_UI_ENABLED`
  - `VITE_AI_CHAT_FEEDBACK_ENABLED`
- Backend
  - `AI_CHAT_META_ENABLED` (maps to `ai.chat-meta.enabled`)
  - `AI_CHAT_META_ROLLOUT_PERCENT` (maps to `ai.chat-meta.rollout-percent`)
- AI Server
  - `AI_CHAT_META_ENABLED`

## 2) Recommended Default Values
- Dev/Staging
  - `AI_CHAT_META_ENABLED=true`
  - `AI_CHAT_META_ROLLOUT_PERCENT=100`
  - `VITE_AI_CHAT_META_UI_ENABLED=true`
  - `VITE_AI_CHAT_FEEDBACK_ENABLED=true`
- Production (phase 1)
  - `AI_CHAT_META_ENABLED=true`
  - `AI_CHAT_META_ROLLOUT_PERCENT=10`
  - `VITE_AI_CHAT_META_UI_ENABLED=true`
  - `VITE_AI_CHAT_FEEDBACK_ENABLED=true`

## 3) Structured Log Events
- `AI_CHAT_REQUEST`
  - keys: `request_id`, `member_id_hash`, `session_id`, `child_id_present`, `intent`, `requested_domains`, `context_mode`, `has_manual_context`, `message_length`
- `AI_CHAT_RESPONSE`
  - keys: `request_id`, `session_id`, `member_id_hash`, `response_mode`, `intent`, `elapsed_ms`, `fallback_code`, `citation_count`, `quick_action_count`, `confidence_bucket`, `meta_exposed`
- `AI_CHAT_FEEDBACK`
  - keys: `request_id`, `session_id`, `member_id_hash`, `rating`, `reason_code`, `response_mode`, `intent`, `is_first_ai_answer`

## 4) Log Verification Procedure
1. `POST /api/ai/chat` 호출 후 `AI_CHAT_REQUEST`, `AI_CHAT_RESPONSE`가 같은 `request_id`로 연속 기록되는지 확인한다.
2. `AI_CHAT_RESPONSE.meta_exposed`가 rollout 설정(`AI_CHAT_META_ROLLOUT_PERCENT`)과 일치하는지 샘플링한다.
3. 장애/타임아웃 시 `response_mode=FALLBACK` 및 `fallback_code` 기록 여부를 확인한다.
4. `POST /api/ai/chat/feedback` 호출 후 `AI_CHAT_FEEDBACK`에 `rating`/`reason_code`가 기록되는지 확인한다.

## 5) Manual QA Scenarios
1. 자녀 미선택 + 성장 질문
   - 기대: `response_mode=CLARIFY`, `missing_fields=["child_selection"]`, 액션 버튼 노출
2. 지역 없는 "어린이집 찾아줘"
   - 기대: `response_mode=CLARIFY`, `missing_fields=["location"]`
3. AI 오류 유도 (timeout/upstream)
   - 기대: `response_mode=FALLBACK`, `fallback_code` 존재, 재시도 액션 노출
4. 일반 답변
   - 기대: `response_mode=ANSWER`, citations 0~3개, quick actions 0~3개
5. 피드백
   - 기대: 👍/👎 제출 성공 및 `AI_CHAT_FEEDBACK` 로그 기록

## 6) Pre-Release Gate
1. AI tests:
   - `./.venv/bin/python -m unittest tests.test_chatbot_api_error_fallback tests.test_chatbot_api_session_continuity tests.test_chatbot_api_location_clarify`
2. Backend tests:
   - `./gradlew test --tests com.childcare.domain.ai.service.AiServiceTest --tests com.childcare.domain.ai.controller.AiControllerTest`
3. Frontend checks:
   - `npx eslint src/store/useStore.js src/components/ChatWindow.jsx src/utils/chatMeta.js`
   - `npm run test:chat-markdown`
   - `node --test tests/chat-meta.test.mjs`
