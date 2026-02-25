ALTER TABLE board_item ADD COLUMN location_scope VARCHAR(20) DEFAULT 'all';

-- 기존 커뮤니티 게시판의 동네 설정이 되어있는 게시글은 neighbor 로 초기화
UPDATE board_item
SET location_scope = 'neighbor'
WHERE bo_seq IN (SELECT bo_seq FROM board WHERE bo_slug = 'community' OR bo_code = 'COMMUNITY')
  AND reg_user_region_code IS NOT NULL;
