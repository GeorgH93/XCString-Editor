CREATE TABLE IF NOT EXISTS pending_shares (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL,
    shared_by_user_id INTEGER NOT NULL,
    shared_with_email VARCHAR(255) NOT NULL,
    can_edit BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES xcstring_files(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(file_id, shared_with_email)
);

CREATE INDEX IF NOT EXISTS idx_pending_shares_file_id ON pending_shares(file_id);
CREATE INDEX IF NOT EXISTS idx_pending_shares_email ON pending_shares(shared_with_email);
CREATE INDEX IF NOT EXISTS idx_pending_shares_created_at ON pending_shares(created_at);