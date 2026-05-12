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
CREATE INDEX IF NOT EXISTS idx_invites_token ON invites(token);
CREATE INDEX IF NOT EXISTS idx_invites_created_by_user_id ON invites(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_invites_expires_at ON invites(expires_at);
