-- Migration: Add region_name column to member table
-- Description: Stores neighborhood name (bname) from Daum Postcode API for community display

ALTER TABLE member ADD COLUMN IF NOT EXISTS region_name VARCHAR(50);

-- Optional: Add index for filtering (uncomment if needed for search performance)
-- CREATE INDEX IF NOT EXISTS idx_member_region_name ON member(region_name);

COMMENT ON COLUMN member.region_name IS 'Neighborhood name from Daum API (e.g., 역삼동)';
