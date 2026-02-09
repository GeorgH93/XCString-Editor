-- Migration: Remove is_used field from presigned_upload_urls table
-- Created: 2026-02-09
-- Description: Remove is_used field to allow presigned URLs to be reused for CI automations.
--              The used_at field now tracks last used timestamp instead of marking as used.

CREATE TABLE IF NOT EXISTS presigned_upload_urls (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    created_by_user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    comment_prefix TEXT,
    FOREIGN KEY (file_id) REFERENCES xcstring_files(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
);


