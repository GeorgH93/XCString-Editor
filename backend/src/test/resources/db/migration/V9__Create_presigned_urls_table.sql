CREATE TABLE IF NOT EXISTS presigned_upload_urls (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    token VARCHAR(64) UNIQUE NOT NULL,
    created_by_user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    comment_prefix TEXT,
    FOREIGN KEY (file_id) REFERENCES xcstring_files(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_presigned_urls_token ON presigned_upload_urls(token);
CREATE INDEX IF NOT EXISTS idx_presigned_urls_file_id ON presigned_upload_urls(file_id);
CREATE INDEX IF NOT EXISTS idx_presigned_urls_expires_at ON presigned_upload_urls(expires_at);
