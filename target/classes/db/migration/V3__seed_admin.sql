-- ═══════════════════════════════════════════════════════════════════
-- Flyway Migration V3 — Admin User Placeholder
-- FightMind AI — fightmind-backend
-- ═══════════════════════════════════════════════════════════════════
-- NOTE: The actual admin INSERT is performed by AdminSeeder.java
-- at application startup (ApplicationRunner bean).
--
-- Reason: The BCrypt hash of ADMIN_PASSWORD is computed at runtime
-- from the environment variable — we cannot compute it in raw SQL
-- without a PostgreSQL pgcrypto extension.
--
-- This migration intentionally creates only a tracking table
-- so Flyway records that this migration version ran successfully.
-- ═══════════════════════════════════════════════════════════════════

-- Tracks whether the admin seed has been applied (prevents duplicate inserts)
CREATE TABLE IF NOT EXISTS admin_seed_log (
    id          SERIAL      PRIMARY KEY,
    seeded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    note        TEXT
);

COMMENT ON TABLE admin_seed_log IS 'Single-row table — presence indicates admin user has been seeded';
