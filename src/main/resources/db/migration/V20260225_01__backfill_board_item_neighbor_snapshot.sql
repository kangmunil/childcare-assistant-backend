-- Migration: Backfill neighbor snapshot fields for existing board items
-- Description:
--   Legacy community posts may have null snapshot fields (reg_user_postcode/region_code/region_name),
--   which causes neighborhood filtering to hide all posts. Backfill from current member profile.

ALTER TABLE board_item
    ADD COLUMN IF NOT EXISTS reg_user_region_name VARCHAR(100);

UPDATE board_item bi
SET reg_user_region_code = CASE
        WHEN bi.reg_user_region_code IS NULL OR BTRIM(bi.reg_user_region_code) = ''
            THEN NULLIF(BTRIM(m.region_code), '')
        ELSE bi.reg_user_region_code
    END,
    reg_user_region_name = CASE
        WHEN bi.reg_user_region_name IS NULL OR BTRIM(bi.reg_user_region_name) = ''
            THEN NULLIF(BTRIM(m.region_name), '')
        ELSE bi.reg_user_region_name
    END,
    reg_user_postcode = CASE
        WHEN bi.reg_user_postcode IS NOT NULL THEN bi.reg_user_postcode
        WHEN NULLIF(REGEXP_REPLACE(COALESCE(m.postcode, ''), '[^0-9]', '', 'g'), '') IS NULL THEN NULL
        WHEN LENGTH(REGEXP_REPLACE(COALESCE(m.postcode, ''), '[^0-9]', '', 'g')) >= 5 THEN
            CAST(SUBSTRING(REGEXP_REPLACE(COALESCE(m.postcode, ''), '[^0-9]', '', 'g') FROM 1 FOR 5) AS INTEGER)
        ELSE
            CAST(REGEXP_REPLACE(COALESCE(m.postcode, ''), '[^0-9]', '', 'g') AS INTEGER)
    END
FROM member m, board b
WHERE bi.reg_id = m.id
  AND b.bo_seq = bi.bo_seq
  AND COALESCE(b.bo_neighbor_yn, 'N') = 'Y'
  AND (
      bi.reg_user_postcode IS NULL
      OR bi.reg_user_region_code IS NULL OR BTRIM(bi.reg_user_region_code) = ''
      OR bi.reg_user_region_name IS NULL OR BTRIM(bi.reg_user_region_name) = ''
  );
