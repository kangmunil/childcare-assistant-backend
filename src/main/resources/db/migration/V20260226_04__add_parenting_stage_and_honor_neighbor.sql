ALTER TABLE member
ADD COLUMN parenting_stage VARCHAR(50),
ADD COLUMN is_honor_neighbor BOOLEAN DEFAULT false;
