-- Migration: Add indexes for file_versions table
-- Created: 2025-07-06
-- Description: Add performance indexes for version history queries

-- Create indexes for better performance
CREATE INDEX idx_file_versions_file_id ON file_versions(file_id);
CREATE INDEX idx_file_versions_created_at ON file_versions(created_at);
CREATE INDEX idx_file_versions_hash ON file_versions(content_hash);