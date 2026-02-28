# AI Chat Meta QA Report Template

- Date:
- Tester:
- Environment: `dev | staging | prod`
- Build/Commit:
- Flags:
  - `AI_CHAT_META_ENABLED=`
  - `AI_CHAT_META_ROLLOUT_PERCENT=`
  - `VITE_AI_CHAT_META_UI_ENABLED=`
  - `VITE_AI_CHAT_FEEDBACK_ENABLED=`
  - `AI_CHAT_META_ENABLED (AI server)=`

## Scenario Results

### 1) Child Not Selected + Growth Question
- Input:
- Expected:
  - `response_mode=CLARIFY`
  - `missing_fields=["child_selection"]`
  - quick action button visible
- Actual:
- Result: `PASS | FAIL`
- Evidence:
  - Screenshot:
  - Log (`request_id`):

### 2) Location Missing ("어린이집 찾아줘")
- Input:
- Expected:
  - `response_mode=CLARIFY`
  - `missing_fields=["location"]`
- Actual:
- Result: `PASS | FAIL`
- Evidence:
  - Screenshot:
  - Log (`request_id`):

### 3) Fallback (timeout/upstream)
- Input / Trigger method:
- Expected:
  - `response_mode=FALLBACK`
  - `fallback_code` exists
  - retry quick action visible
- Actual:
- Result: `PASS | FAIL`
- Evidence:
  - Screenshot:
  - Log (`request_id`):

### 4) Normal Answer + Citations
- Input:
- Expected:
  - `response_mode=ANSWER`
  - citations rendered (0~3)
- Actual:
- Result: `PASS | FAIL`
- Evidence:
  - Screenshot:
  - Log (`request_id`):

### 5) Feedback (👍/👎 + reason_code)
- Input:
- Expected:
  - API success
  - `AI_CHAT_FEEDBACK` log emitted with `rating`/`reason_code`
- Actual:
- Result: `PASS | FAIL`
- Evidence:
  - Screenshot:
  - Log (`request_id` / `session_id`):

## Rollout Checks
- Sample users checked:
- `meta_exposed=true` sample count:
- `meta_exposed=false` sample count:
- Verdict:

## Summary
- Overall Result: `PASS | FAIL`
- Blocking Issues:
- Non-blocking Issues:
- Action Items / Owner / ETA:
