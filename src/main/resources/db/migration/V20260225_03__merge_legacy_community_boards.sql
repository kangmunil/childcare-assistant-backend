-- Migration: Merge legacy split community boards into the canonical `community` board
-- Description:
--   Frontend routes use /boards/community/*, but historical data may exist in separate
--   qna/daily/tip boards. Move those posts into the `community` board so list/detail/comment
--   flows work consistently with the current frontend.

WITH target_board AS (
    SELECT bo_seq
    FROM board
    WHERE bo_slug = 'community'
    ORDER BY bo_seq
    LIMIT 1
),
source_boards AS (
    SELECT bo_seq, LOWER(BTRIM(bo_code)) AS source_code
    FROM board
    WHERE LOWER(BTRIM(COALESCE(bo_code, ''))) IN ('qna', 'daily', 'tip')
)
UPDATE board_item bi
SET bo_seq = tb.bo_seq,
    bi_category = CASE
        WHEN bi.bi_category IS NULL OR BTRIM(bi.bi_category) = '' THEN sb.source_code
        ELSE LOWER(BTRIM(bi.bi_category))
    END
FROM source_boards sb
CROSS JOIN target_board tb
WHERE bi.bo_seq = sb.bo_seq
  AND bi.bo_seq <> tb.bo_seq;
