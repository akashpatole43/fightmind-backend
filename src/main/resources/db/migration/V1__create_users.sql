-- ═══════════════════════════════════════════════════════════════════
-- Flyway Migration V1 — Create Users Table
-- FightMind AI — fightmind-backend
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,

    -- Auth credentials
    email       VARCHAR(255)    NOT NULL UNIQUE,
    username    VARCHAR(100)    NOT NULL UNIQUE,
    password    VARCHAR(255),                           -- NULL for Google-only accounts (no password set)

    -- OAuth2 provider
    provider    VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',   -- 'LOCAL' | 'GOOGLE'
    google_id   VARCHAR(200),                           -- Google subject ID (unique per Google account)

    -- Role based access control
    role        VARCHAR(20)     NOT NULL DEFAULT 'ROLE_USER',  -- 'ROLE_USER' | 'ROLE_ADMIN'

    -- Martial arts profile
    skill_level VARCHAR(20)     DEFAULT 'UNKNOWN',     -- 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'UNKNOWN'

    -- Timestamps
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ── Indexes ────────────────────────────────────────────────────────────────
CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_role     ON users (role);
CREATE INDEX idx_users_provider ON users (provider);

-- Partial index: only index google_id for OAuth users (saves space)
CREATE INDEX idx_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;

-- ── Constraint: Google-only users must have a google_id ───────────────────
ALTER TABLE users
    ADD CONSTRAINT chk_google_id_required
    CHECK (
        (provider = 'GOOGLE' AND google_id IS NOT NULL)
        OR provider != 'GOOGLE'
    );

-- ── Trigger: auto-update updated_at on every row update ───────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── Comments (for DB documentation) ──────────────────────────────────────
COMMENT ON TABLE  users               IS 'FightMind registered users (local + Google OAuth)';
COMMENT ON COLUMN users.provider      IS 'Auth provider: LOCAL (email/password) or GOOGLE (OAuth2)';
COMMENT ON COLUMN users.google_id     IS 'Google subject ID — populated for GOOGLE users only';
COMMENT ON COLUMN users.password      IS 'BCrypt-hashed password — NULL for Google-only accounts';
COMMENT ON COLUMN users.skill_level   IS 'Martial arts skill level inferred from chat history';
