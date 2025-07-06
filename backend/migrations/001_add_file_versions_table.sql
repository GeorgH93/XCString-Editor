-- Migration: Add file_versions table for version history
-- Created: 2025-07-06
-- Description: Add version history tracking for xcstring files

-- Create file version history table
CREATE TABLE file_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL,
    version_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    comment TEXT,
    created_by_user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content_hash VARCHAR(64) NOT NULL, -- SHA-256 hash for deduplication
    size_bytes INTEGER NOT NULL,
    FOREIGN KEY (file_id) REFERENCES xcstring_files(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    UNIQUE(file_id, version_number)
);

-- Create indexes for better performance
CREATE INDEX idx_file_versions_file_id ON file_versions(file_id);
CREATE INDEX idx_file_versions_created_at ON file_versions(created_at);
CREATE INDEX idx_file_versions_hash ON file_versions(content_hash);