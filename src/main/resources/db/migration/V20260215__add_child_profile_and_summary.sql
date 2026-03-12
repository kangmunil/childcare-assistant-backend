CREATE TABLE IF NOT EXISTS child_profile (
    ch_seq BIGINT PRIMARY KEY REFERENCES child(ch_seq),
    health JSONB NOT NULL DEFAULT '{}'::jsonb,
    routine JSONB NOT NULL DEFAULT '{}'::jsonb,
    development JSONB NOT NULL DEFAULT '{}'::jsonb,
    education JSONB NOT NULL DEFAULT '{}'::jsonb,
    safety JSONB NOT NULL DEFAULT '{}'::jsonb,
    misc JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID NULL
);

CREATE TABLE IF NOT EXISTS child_profile_summary (
    ch_seq BIGINT PRIMARY KEY REFERENCES child(ch_seq),
    summary_text TEXT NOT NULL,
    summary_version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_child_profile_updated_at ON child_profile(updated_at);
CREATE INDEX IF NOT EXISTS idx_child_profile_summary_updated_at ON child_profile_summary(updated_at);

INSERT INTO child_profile (ch_seq)
SELECT c.ch_seq
FROM child c
ON CONFLICT (ch_seq) DO NOTHING;

INSERT INTO child_profile_summary (ch_seq, summary_text, summary_version)
SELECT c.ch_seq, '프로필 요약이 아직 생성되지 않았습니다.', 1
FROM child c
ON CONFLICT (ch_seq) DO NOTHING;
