-- Migration: Add board_item writer region name snapshot for stable community display
-- Description:
--   1) Stores writer region_name at post creation time
--   2) Prevents historical posts from changing displayed neighborhood when member moves

ALTER TABLE board_item
    ADD COLUMN IF NOT EXISTS reg_user_region_name VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_board_item_neighbor_region_name
    ON board_item(bo_seq, reg_user_region_name);

UPDATE board_item bi
SET reg_user_region_name = m.region_name
FROM member m
WHERE bi.reg_id = m.id
  AND (bi.reg_user_region_name IS NULL OR bi.reg_user_region_name = '')
  AND m.region_name IS NOT NULL
  AND m.region_name <> '';

COMMENT ON COLUMN board_item.reg_user_region_name IS 'Snapshot of writer region name at post creation time';
