-- Migration: Add region_code columns for precise neighborhood matching
-- Description:
--   1) member.region_code stores legal/admin dong code from Kakao Local API
--   2) board_item.reg_user_region_code snapshots writer's region code on post creation
--   3) Existing board items are backfilled from current member.region_code when possible

ALTER TABLE member
    ADD COLUMN IF NOT EXISTS region_code VARCHAR(20);

ALTER TABLE board_item
    ADD COLUMN IF NOT EXISTS reg_user_region_code VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_member_region_code
    ON member(region_code);

CREATE INDEX IF NOT EXISTS idx_board_item_neighbor_region
    ON board_item(bo_seq, reg_user_region_code);

CREATE INDEX IF NOT EXISTS idx_board_item_neighbor_postcode
    ON board_item(bo_seq, reg_user_postcode);

UPDATE board_item bi
SET reg_user_region_code = m.region_code
FROM member m
WHERE bi.reg_id = m.id
  AND bi.reg_user_region_code IS NULL
  AND m.region_code IS NOT NULL
  AND m.region_code <> '';

COMMENT ON COLUMN member.region_code IS 'Kakao region code (B/H), used for precise community neighborhood matching';
COMMENT ON COLUMN board_item.reg_user_region_code IS 'Snapshot of writer region code at posting time';
