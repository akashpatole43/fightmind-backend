package com.fightmind.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * AdminSeeder — runs once at startup after all Flyway migrations are complete.
 *
 * Safely seeds the fightmind_admin user by:
 *  1. Checking admin_seed_log (created by V3 migration) — if a row exists, skip
 *  2. BCrypt-hashing the ADMIN_PASSWORD env variable at runtime
 *  3. Inserting the admin row into the users table
 *  4. Writing a row to admin_seed_log to prevent re-seeding on next startup
 *
 * Why not do this in SQL (V3__seed_admin.sql)?
 *  PostgreSQL cannot call BCrypt without pgcrypto extension.
 *  We deliberately avoid storing plain-text passwords anywhere in source code or SQL files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final JdbcTemplate    jdbc;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;       // plain-text from .env — never logged, never stored as-is

    @Value("${admin.email}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {
        log.info("AdminSeeder — checking if admin user needs to be seeded");

        // ── Guard: skip if already seeded ────────────────────────────────
        Integer seedCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_seed_log", Integer.class);

        if (seedCount != null && seedCount > 0) {
            log.info("AdminSeeder — admin already seeded, skipping");
            return;
        }

        // ── Guard: skip if admin user already exists in users table ──────
        Integer userCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class, adminUsername);

        if (userCount != null && userCount > 0) {
            log.warn("AdminSeeder — admin user '{}' already exists in DB, recording seed log",
                    adminUsername);
            recordSeedLog("Admin user already existed — seed log created retroactively");
            return;
        }

        // ── Insert admin user (password BCrypt-hashed at runtime) ────────
        String hashedPassword = passwordEncoder.encode(adminPassword);

        jdbc.update("""
                INSERT INTO users (email, username, password, provider, role, skill_level)
                VALUES (?, ?, ?, 'LOCAL', 'ROLE_ADMIN', 'ADVANCED')
                """,
                adminEmail, adminUsername, hashedPassword);

        log.info("AdminSeeder — admin user '{}' created successfully with ROLE_ADMIN",
                adminUsername);

        // ── Mark as seeded so this never runs again ───────────────────────
        recordSeedLog("Initial admin seed — username: " + adminUsername);
    }

    private void recordSeedLog(String note) {
        jdbc.update("INSERT INTO admin_seed_log (note) VALUES (?)", note);
    }
}
