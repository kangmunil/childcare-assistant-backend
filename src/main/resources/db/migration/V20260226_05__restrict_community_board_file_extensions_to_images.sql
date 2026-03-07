-- Migration: Restrict community board upload extensions to image-only
-- Description:
--   Community UI now uploads images only. Keep DB board config aligned with
--   server-side enforcement to avoid environment-specific drift.

UPDATE board
SET bo_file_extension = 'jpg,jpeg,png,gif,webp'
WHERE (bo_slug = 'community' OR UPPER(COALESCE(bo_code, '')) = 'COMMUNITY')
  AND COALESCE(TRIM(bo_file_extension), '') <> 'jpg,jpeg,png,gif,webp';
