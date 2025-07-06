-- Migration: Create migrations tracking table
-- Created: 2025-07-06
-- Description: Track which database migrations have been applied

CREATE TABLE migrations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    migration VARCHAR(255) NOT NULL UNIQUE,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);