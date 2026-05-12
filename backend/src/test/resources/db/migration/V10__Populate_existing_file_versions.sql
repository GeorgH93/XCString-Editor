-- H2-compatible: populate initial file versions for existing files (no-op on empty test DB)
INSERT INTO file_versions (file_id, version_number, content, comment, created_by_user_id, created_at, content_hash, size_bytes)
SELECT
    f.id,
    1,
    f.content,
    'Initial version (migrated)',
    f.user_id,
    f.created_at,
    RAWTOHEX(RAND()),
    LENGTH(f.content)
FROM xcstring_files f
WHERE NOT EXISTS (
    SELECT 1 FROM file_versions fv WHERE fv.file_id = f.id
);
