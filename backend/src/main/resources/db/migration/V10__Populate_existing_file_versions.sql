-- Create initial version entries for any xcstring_files that don't have versions yet
-- This handles migration from existing PHP databases
INSERT OR IGNORE INTO file_versions (file_id, version_number, content, comment, created_by_user_id, created_at, content_hash, size_bytes)
SELECT 
    f.id,
    1,
    f.content,
    'Initial version (migrated)',
    f.user_id,
    f.created_at,
    hex(randomblob(32)),
    length(f.content)
FROM xcstring_files f
WHERE NOT EXISTS (
    SELECT 1 FROM file_versions fv WHERE fv.file_id = f.id
);
