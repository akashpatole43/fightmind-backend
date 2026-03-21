-- ═══════════════════════════════════════════════════════════════════
-- Flyway Migration V2 — Create Chat History Table
-- FightMind AI — fightmind-backend
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE chat_messages (
    id          BIGSERIAL       PRIMARY KEY,

    -- Relationship to the user (cascade delete — remove history when user is deleted)
    user_id     BIGINT          NOT NULL
                    REFERENCES users(id) ON DELETE CASCADE,

    -- The user's question and the AI's answer
    query       TEXT            NOT NULL,
    answer      TEXT            NOT NULL,

    -- AI pipeline metadata (from Python service response)
    intent      VARCHAR(50),                            -- e.g. 'TECHNIQUE', 'RULES', 'GENERAL'
    sport       VARCHAR(50),                            -- e.g. 'boxing', 'muay_thai', 'karate'
    confidence  FLOAT,                                  -- intent confidence score [0.0 – 1.0]
    cached      BOOLEAN         NOT NULL DEFAULT FALSE, -- TRUE if this response was served from Redis cache

    -- Optional: image that was sent with the query
    image_url   VARCHAR(500),                           -- Cloudinary URL (null if text-only query)

    -- Tracing — links this message to a specific request trace
    trace_id    VARCHAR(64),                            -- Micrometer traceId for debugging

    -- Timestamps
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ── Indexes ────────────────────────────────────────────────────────────────
-- Most common query: "get all messages for user X" — highly selective
CREATE INDEX idx_chat_user_id      ON chat_messages (user_id);

-- For admin history view: "most recent messages first"
CREATE INDEX idx_chat_created_at   ON chat_messages (created_at DESC);

-- Removed partial index idx_chat_today because PostgreSQL requires immutable predicates.
-- The idx_chat_created_at index below perfectly handles time-range queries anyway.

-- Composite: for paginated user history (common pattern: WHERE user_id = ? ORDER BY created_at DESC)
CREATE INDEX idx_chat_user_created ON chat_messages (user_id, created_at DESC);

-- ── Comments ──────────────────────────────────────────────────────────────
COMMENT ON TABLE  chat_messages            IS 'Persisted Q&A pairs between users and the FightMind AI';
COMMENT ON COLUMN chat_messages.intent     IS 'Detected query intent from the Python pipeline Level 1';
COMMENT ON COLUMN chat_messages.sport      IS 'Detected sport context (boxing, muay_thai, karate, etc.)';
COMMENT ON COLUMN chat_messages.confidence IS 'Intent classification confidence score from Python pipeline';
COMMENT ON COLUMN chat_messages.cached     IS 'TRUE if response was served from Redis (not a Gemini call)';
COMMENT ON COLUMN chat_messages.trace_id   IS 'Micrometer distributed traceId for cross-service debugging';
