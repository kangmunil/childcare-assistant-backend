-- Migration: Ensure community board is enabled
-- Description:
--   Prevents BOARD_002 on /boards/community/* when the board row exists
--   but bo_use_yn is null/blank/lowercase or disabled in an environment DB.

UPDATE board
SET bo_use_yn = 'Y'
WHERE (bo_slug = 'community' OR UPPER(COALESCE(bo_code, '')) = 'COMMUNITY')
  AND COALESCE(UPPER(TRIM(bo_use_yn)), '') <> 'Y';
