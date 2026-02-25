UPDATE board_item
SET location_scope = 'all'
WHERE location_scope IS NULL
   OR BTRIM(location_scope) = ''
   OR LOWER(BTRIM(location_scope)) NOT IN ('all', 'neighbor');

UPDATE board_item
SET location_scope = LOWER(BTRIM(location_scope))
WHERE location_scope IS NOT NULL
  AND LOWER(BTRIM(location_scope)) IN ('all', 'neighbor')
  AND location_scope <> LOWER(BTRIM(location_scope));

ALTER TABLE board_item
ALTER COLUMN location_scope SET DEFAULT 'all';
