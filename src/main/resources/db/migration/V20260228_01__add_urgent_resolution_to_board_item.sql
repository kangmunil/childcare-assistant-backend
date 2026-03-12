ALTER TABLE board_item
    ADD COLUMN IF NOT EXISTS urgent_resolved_yn VARCHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS urgent_resolved_date TIMESTAMP;

UPDATE board_item
SET urgent_resolved_yn = 'N'
WHERE urgent_resolved_yn IS NULL;
