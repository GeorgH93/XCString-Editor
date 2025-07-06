-- Migration: Add invites table for invite system
-- Created: 2025-07-06
-- Description: Creates table for storing invite tokens that allow registration when registration is disabled

CREATE TABLE IF NOT EXISTS invites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    token VARCHAR(128) UNIQUE NOT NULL,
    created_by_user_id INTEGER NOT NULL,
    email VARCHAR(255),
    used_by_user_id INTEGER,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (used_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_invites_token ON invites(token);

CREATE INDEX idx_invites_created_by ON invites(created_by_user_id);

CREATE INDEX idx_invites_expires ON invites(expires_at);

CREATE INDEX idx_invites_used_by ON invites(used_by_user_id);