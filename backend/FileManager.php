<?php

class FileManager {
    private $db;
    private $config;
    
    public function __construct($db, $config) {
        $this->db = $db;
        $this->config = $config;
        
        // Ensure version history table exists
        $this->ensureVersionHistoryTable();
    }
    
    private function ensureVersionHistoryTable() {
        // Check if file_versions table exists
        try {
            $this->db->fetchOne("SELECT COUNT(*) FROM file_versions LIMIT 1");
            return; // Table exists
        } catch (Exception $e) {
            // Table doesn't exist, create it
        }
        
        try {
            // Create file_versions table
            $sql = "CREATE TABLE file_versions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_id INTEGER NOT NULL,
                version_number INTEGER NOT NULL,
                content TEXT NOT NULL,
                comment TEXT,
                created_by_user_id INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                content_hash VARCHAR(64) NOT NULL,
                size_bytes INTEGER NOT NULL,
                FOREIGN KEY (file_id) REFERENCES xcstring_files(id) ON DELETE CASCADE,
                FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
                UNIQUE(file_id, version_number)
            )";
            
            // Adapt SQL for different database drivers
            if ($this->config['database']['driver'] === 'mysql') {
                $sql = str_replace('INTEGER PRIMARY KEY AUTOINCREMENT', 'INT AUTO_INCREMENT PRIMARY KEY', $sql);
                $sql = str_replace('TIMESTAMP DEFAULT CURRENT_TIMESTAMP', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP', $sql);
            } elseif ($this->config['database']['driver'] === 'postgres') {
                $sql = str_replace('INTEGER PRIMARY KEY AUTOINCREMENT', 'SERIAL PRIMARY KEY', $sql);
            }
            
            $this->db->execute($sql);
            
            // Create indexes
            $indexes = [
                "CREATE INDEX idx_file_versions_file_id ON file_versions(file_id)",
                "CREATE INDEX idx_file_versions_created_at ON file_versions(created_at)", 
                "CREATE INDEX idx_file_versions_hash ON file_versions(content_hash)"
            ];
            
            foreach ($indexes as $indexSql) {
                try {
                    $this->db->execute($indexSql);
                } catch (Exception $e) {
                    // Ignore index creation errors - they might already exist
                }
            }
            
            // Populate existing files with initial versions
            $this->populateExistingFileVersions();
            
            // Also ensure presigned URLs table exists
            $this->ensurePresignedUrlsTable();
            
        } catch (Exception $e) {
            error_log("Failed to create version history table: " . $e->getMessage());
            // Don't throw - allow app to continue without version history
        }
    }
    
    private function populateExistingFileVersions() {
        try {
            // Get all existing files that don't have versions yet
            $files = $this->db->fetchAll("
                SELECT f.id, f.content, f.user_id, f.created_at 
                FROM xcstring_files f 
                LEFT JOIN file_versions fv ON f.id = fv.file_id 
                WHERE fv.id IS NULL
            ");
            
            foreach ($files as $file) {
                $contentHash = hash('sha256', $file['content']);
                $sizeBytes = strlen($file['content']);
                
                // Create initial version for existing file
                $this->db->execute("
                    INSERT INTO file_versions 
                    (file_id, version_number, content, comment, created_by_user_id, created_at, content_hash, size_bytes) 
                    VALUES (?, 1, ?, 'Initial version (auto-created)', ?, ?, ?, ?)
                ", [
                    $file['id'], 
                    $file['content'], 
                    $file['user_id'], 
                    $file['created_at'],
                    $contentHash, 
                    $sizeBytes
                ]);
            }
            
            if (count($files) > 0) {
                error_log("Created initial versions for " . count($files) . " existing files");
            }
        } catch (Exception $e) {
            error_log("Failed to populate existing file versions: " . $e->getMessage());
            // Don't throw - version history will work for new files
        }
    }
    
    private function ensurePresignedUrlsTable() {
        try {
            // Check if table exists
            $this->db->fetchOne("SELECT COUNT(*) FROM presigned_upload_urls LIMIT 1");
            return; // Table exists
        } catch (Exception $e) {
            // Table doesn't exist, create it
        }
        
        try {
            // Create presigned_upload_urls table
            $sql = "CREATE TABLE presigned_upload_urls (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_id INTEGER NOT NULL,
                token VARCHAR(64) NOT NULL UNIQUE,
                created_by_user_id INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NOT NULL,
                is_used BOOLEAN DEFAULT FALSE,
                used_at TIMESTAMP NULL,
                comment_prefix TEXT,
                FOREIGN KEY (file_id) REFERENCES xcstring_files(id) ON DELETE CASCADE,
                FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
            )";
            
            // Adapt SQL for different database drivers
            if ($this->config['database']['driver'] === 'mysql') {
                $sql = str_replace('INTEGER PRIMARY KEY AUTOINCREMENT', 'INT AUTO_INCREMENT PRIMARY KEY', $sql);
                $sql = str_replace('BOOLEAN', 'TINYINT(1)', $sql);
            } elseif ($this->config['database']['driver'] === 'postgres') {
                $sql = str_replace('INTEGER PRIMARY KEY AUTOINCREMENT', 'SERIAL PRIMARY KEY', $sql);
            }
            
            $this->db->execute($sql);
            
            // Create indexes
            $indexes = [
                "CREATE INDEX idx_presigned_urls_token ON presigned_upload_urls(token)",
                "CREATE INDEX idx_presigned_urls_file_id ON presigned_upload_urls(file_id)",
                "CREATE INDEX idx_presigned_urls_expires_at ON presigned_upload_urls(expires_at)"
            ];
            
            foreach ($indexes as $indexSql) {
                try {
                    $this->db->execute($indexSql);
                } catch (Exception $e) {
                    // Ignore index creation errors - they might already exist
                }
            }
            
        } catch (Exception $e) {
            error_log("Failed to create presigned URLs table: " . $e->getMessage());
            // Don't throw - allow app to continue without presigned URLs
        }
    }
    
    public function saveFile($userId, $name, $content, $isPublic = false, $comment = null) {
        // Validate file size
        if (strlen($content) > $this->config['files']['max_file_size']) {
            throw new Exception('File size exceeds maximum allowed size');
        }
        
        // Check user's file limit
        $fileCount = $this->db->fetchOne(
            'SELECT COUNT(*) as count FROM xcstring_files WHERE user_id = ?',
            [$userId]
        )['count'];
        
        if ($fileCount >= $this->config['files']['max_files_per_user']) {
            throw new Exception('Maximum number of files reached');
        }
        
        // Validate content is valid JSON
        $parsed = json_decode($content, true);
        if (!$parsed) {
            throw new Exception('Invalid xcstring content');
        }
        
        try {
            $this->db->beginTransaction();
            
            // Insert file
            $this->db->execute(
                'INSERT INTO xcstring_files (user_id, name, content, is_public, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)',
                [$userId, $name, $content, $isPublic ? 1 : 0]
            );
            
            $fileId = $this->db->lastInsertId();
            
            // Create initial version
            $this->createFileVersion($fileId, $content, $userId, $comment ?: 'Initial version');
            
            $this->db->commit();
            return $fileId;
            
        } catch (Exception $e) {
            $this->db->rollback();
            throw $e;
        }
    }
    
    public function updateFile($fileId, $userId, $content, $comment = null) {
        // Check if user owns the file or has edit permission
        if (!$this->canEditFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        // Validate file size
        if (strlen($content) > $this->config['files']['max_file_size']) {
            throw new Exception('File size exceeds maximum allowed size');
        }
        
        // Validate content is valid JSON
        $parsed = json_decode($content, true);
        if (!$parsed) {
            throw new Exception('Invalid xcstring content');
        }
        
        // Get current content to check if it has actually changed
        $currentFile = $this->db->fetchOne(
            'SELECT content FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$currentFile) {
            throw new Exception('File not found');
        }
        
        // Only update and create version if content has changed
        $currentHash = hash('sha256', $currentFile['content']);
        $newHash = hash('sha256', $content);
        
        if ($currentHash !== $newHash) {
            try {
                $this->db->beginTransaction();
                
                // Update file
                $this->db->execute(
                    'UPDATE xcstring_files SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
                    [$content, $fileId]
                );
                
                // Create new version
                $this->createFileVersion($fileId, $content, $userId, $comment);
                
                $this->db->commit();
            } catch (Exception $e) {
                $this->db->rollback();
                throw $e;
            }
        }
        
        return true;
    }
    
    public function getFile($fileId, $userId = null) {
        $sql = 'SELECT f.*, u.name as owner_name FROM xcstring_files f 
                JOIN users u ON f.user_id = u.id 
                WHERE f.id = ?';
        $params = [$fileId];
        
        $file = $this->db->fetchOne($sql, $params);
        
        if (!$file) {
            throw new Exception('File not found');
        }
        
        // Check permissions
        if (!$this->canViewFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        return $file;
    }
    
    public function getUserFiles($userId) {
        return $this->db->fetchAll(
            'SELECT id, name, is_public, created_at, updated_at FROM xcstring_files 
             WHERE user_id = ? ORDER BY updated_at DESC',
            [$userId]
        );
    }
    
    public function getSharedFiles($userId) {
        return $this->db->fetchAll(
            'SELECT f.id, f.name, f.created_at, f.updated_at, u.name as owner_name, fs.can_edit
             FROM xcstring_files f
             JOIN file_shares fs ON f.id = fs.file_id
             JOIN users u ON f.user_id = u.id
             WHERE fs.shared_with_user_id = ?
             ORDER BY f.updated_at DESC',
            [$userId]
        );
    }
    
    public function getPublicFiles($limit = 50) {
        return $this->db->fetchAll(
            'SELECT f.id, f.name, f.created_at, f.updated_at, u.name as owner_name
             FROM xcstring_files f
             JOIN users u ON f.user_id = u.id
             WHERE f.is_public = 1
             ORDER BY f.updated_at DESC
             LIMIT ?',
            [$limit]
        );
    }
    
    public function deleteFile($fileId, $userId) {
        // Check if user owns the file
        $file = $this->db->fetchOne(
            'SELECT user_id FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$file || $file['user_id'] != $userId) {
            throw new Exception('Permission denied');
        }
        
        $this->db->execute('DELETE FROM xcstring_files WHERE id = ?', [$fileId]);
        return true;
    }
    
    public function shareFile($fileId, $ownerId, $shareWithEmail, $canEdit = false) {
        // Verify file ownership
        $file = $this->db->fetchOne(
            'SELECT user_id FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$file || $file['user_id'] != $ownerId) {
            throw new Exception('Permission denied');
        }
        
        // Find user to share with
        $shareWithUser = $this->db->fetchOne(
            'SELECT id FROM users WHERE email = ?',
            [$shareWithEmail]
        );
        
        if (!$shareWithUser) {
            throw new Exception('User not found');
        }
        
        // Don't allow sharing with self
        if ($shareWithUser['id'] == $ownerId) {
            throw new Exception('Cannot share with yourself');
        }
        
        // Insert or update share
        $existing = $this->db->fetchOne(
            'SELECT id FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?',
            [$fileId, $shareWithUser['id']]
        );
        
        if ($existing) {
            $this->db->execute(
                'UPDATE file_shares SET can_edit = ? WHERE id = ?',
                [$canEdit ? 1 : 0, $existing['id']]
            );
        } else {
            $this->db->execute(
                'INSERT INTO file_shares (file_id, shared_with_user_id, can_edit) VALUES (?, ?, ?)',
                [$fileId, $shareWithUser['id'], $canEdit ? 1 : 0]
            );
        }
        
        return true;
    }
    
    public function unshareFile($fileId, $ownerId, $unshareWithUserId) {
        // Verify file ownership
        $file = $this->db->fetchOne(
            'SELECT user_id FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$file || $file['user_id'] != $ownerId) {
            throw new Exception('Permission denied');
        }
        
        $this->db->execute(
            'DELETE FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?',
            [$fileId, $unshareWithUserId]
        );
        
        return true;
    }
    
    public function getFileShares($fileId, $ownerId) {
        // Verify file ownership
        $file = $this->db->fetchOne(
            'SELECT user_id FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$file || $file['user_id'] != $ownerId) {
            throw new Exception('Permission denied');
        }
        
        return $this->db->fetchAll(
            'SELECT fs.*, u.email, u.name FROM file_shares fs
             JOIN users u ON fs.shared_with_user_id = u.id
             WHERE fs.file_id = ?
             ORDER BY u.name',
            [$fileId]
        );
    }
    
    private function canViewFile($fileId, $userId) {
        $file = $this->db->fetchOne(
            'SELECT user_id, is_public FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$file) {
            return false;
        }
        
        // Owner can always view
        if ($userId && $file['user_id'] == $userId) {
            return true;
        }
        
        // Public files can be viewed by anyone
        if ($file['is_public']) {
            return true;
        }
        
        // Check if file is shared with user
        if ($userId) {
            $share = $this->db->fetchOne(
                'SELECT id FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?',
                [$fileId, $userId]
            );
            if ($share) {
                return true;
            }
        }
        
        return false;
    }
    
    private function canEditFile($fileId, $userId) {
        $file = $this->db->fetchOne(
            'SELECT user_id FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$file) {
            return false;
        }
        
        // Owner can always edit
        if ($file['user_id'] == $userId) {
            return true;
        }
        
        // Check if user has edit permission
        $share = $this->db->fetchOne(
            'SELECT can_edit FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?',
            [$fileId, $userId]
        );
        
        return $share && $share['can_edit'];
    }
    
    // Version History Methods
    
    private function createFileVersion($fileId, $content, $userId, $comment = null) {
        // Get the next version number
        $latestVersion = $this->db->fetchOne(
            'SELECT MAX(version_number) as max_version FROM file_versions WHERE file_id = ?',
            [$fileId]
        );
        
        $versionNumber = ($latestVersion['max_version'] ?? 0) + 1;
        $contentHash = hash('sha256', $content);
        $sizeBytes = strlen($content);
        
        // Check if this exact content already exists (deduplication)
        $existingVersion = $this->db->fetchOne(
            'SELECT id FROM file_versions WHERE file_id = ? AND content_hash = ?',
            [$fileId, $contentHash]
        );
        
        if (!$existingVersion) {
            $this->db->execute(
                'INSERT INTO file_versions (file_id, version_number, content, comment, created_by_user_id, content_hash, size_bytes) 
                 VALUES (?, ?, ?, ?, ?, ?, ?)',
                [$fileId, $versionNumber, $content, $comment, $userId, $contentHash, $sizeBytes]
            );
        }
        
        return $versionNumber;
    }
    
    public function getFileVersions($fileId, $userId = null) {
        // Check if user can view this file
        if (!$this->canViewFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        return $this->db->fetchAll(
            'SELECT fv.id, fv.version_number, fv.comment, fv.created_at, fv.size_bytes,
                    u.name as created_by_name, u.email as created_by_email
             FROM file_versions fv
             JOIN users u ON fv.created_by_user_id = u.id
             WHERE fv.file_id = ?
             ORDER BY fv.version_number DESC',
            [$fileId]
        );
    }
    
    public function getFileVersion($fileId, $versionNumber, $userId = null) {
        // Check if user can view this file
        if (!$this->canViewFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        $version = $this->db->fetchOne(
            'SELECT fv.*, u.name as created_by_name, u.email as created_by_email
             FROM file_versions fv
             JOIN users u ON fv.created_by_user_id = u.id
             WHERE fv.file_id = ? AND fv.version_number = ?',
            [$fileId, $versionNumber]
        );
        
        if (!$version) {
            throw new Exception('Version not found');
        }
        
        return $version;
    }
    
    public function revertToVersion($fileId, $versionNumber, $userId, $comment = null) {
        // Check if user can edit this file
        if (!$this->canEditFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        // Get the version content
        $version = $this->getFileVersion($fileId, $versionNumber, $userId);
        
        // Update file with version content and create new version
        $revertComment = $comment ?: "Reverted to version $versionNumber";
        return $this->updateFile($fileId, $userId, $version['content'], $revertComment);
    }
    
    public function deleteFileVersion($fileId, $versionNumber, $userId) {
        // Check if user owns the file
        $file = $this->db->fetchOne(
            'SELECT user_id FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        if (!$file || $file['user_id'] != $userId) {
            throw new Exception('Permission denied');
        }
        
        // Don't allow deletion of the latest version
        $latestVersion = $this->db->fetchOne(
            'SELECT MAX(version_number) as max_version FROM file_versions WHERE file_id = ?',
            [$fileId]
        );
        
        if ($versionNumber == $latestVersion['max_version']) {
            throw new Exception('Cannot delete the latest version');
        }
        
        // Don't allow deletion if only one version exists
        $versionCount = $this->db->fetchOne(
            'SELECT COUNT(*) as count FROM file_versions WHERE file_id = ?',
            [$fileId]
        );
        
        if ($versionCount['count'] <= 1) {
            throw new Exception('Cannot delete the only version');
        }
        
        $this->db->execute(
            'DELETE FROM file_versions WHERE file_id = ? AND version_number = ?',
            [$fileId, $versionNumber]
        );
        
        return true;
    }
    
    public function getFileVersionStats($fileId, $userId = null) {
        // Check if user can view this file
        if (!$this->canViewFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        return $this->db->fetchOne(
            'SELECT 
                COUNT(*) as total_versions,
                MIN(created_at) as first_version_date,
                MAX(created_at) as latest_version_date,
                SUM(size_bytes) as total_size_bytes,
                COUNT(DISTINCT created_by_user_id) as unique_contributors
             FROM file_versions 
             WHERE file_id = ?',
            [$fileId]
        );
    }
    
    public function generatePresignedUploadUrl($fileId, $userId, $commentPrefix = null) {
        // Check if user can edit this file
        if (!$this->canEditFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        // Generate a secure random token
        $token = bin2hex(random_bytes(32));
        
        // Set expiration to 1 year from now
        $expiresAt = date('Y-m-d H:i:s', strtotime('+1 year'));
        
        // Insert presigned URL record
        $this->db->execute('
            INSERT INTO presigned_upload_urls 
            (file_id, token, created_by_user_id, expires_at, comment_prefix) 
            VALUES (?, ?, ?, ?, ?)',
            [$fileId, $token, $userId, $expiresAt, $commentPrefix]
        );
        
        return [
            'token' => $token,
            'expires_at' => $expiresAt,
            'upload_url' => $this->buildPresignedUploadUrl($token)
        ];
    }
    
    private function buildPresignedUploadUrl($token) {
        $baseUrl = $this->config['app']['base_url'] ?? 'http://localhost:8080';
        return $baseUrl . '/backend/index.php/upload/' . $token;
    }
    
    public function uploadViaPresignedUrl($token, $content, $comment = null) {
        // Get presigned URL record  
        $currentTime = date('Y-m-d H:i:s');
        $presignedUrl = $this->db->fetchOne('
            SELECT * FROM presigned_upload_urls 
            WHERE token = ? AND is_used = FALSE AND expires_at > ?',
            [$token, $currentTime]
        );
        
        if (!$presignedUrl) {
            throw new Exception('Invalid or expired upload token');
        }
        
        // Build comment with prefix if provided
        $finalComment = $comment;
        if ($presignedUrl['comment_prefix']) {
            $finalComment = $presignedUrl['comment_prefix'] . ($comment ? ' - ' . $comment : '');
        }
        if (!$finalComment) {
            $finalComment = 'Uploaded via presigned URL';
        }
        
        // Update the file (creates new version)
        $this->updateFile($presignedUrl['file_id'], $presignedUrl['created_by_user_id'], $content, $finalComment);
        
        // Mark presigned URL as used
        $usedAt = date('Y-m-d H:i:s');
        $this->db->execute('
            UPDATE presigned_upload_urls 
            SET is_used = TRUE, used_at = ? 
            WHERE token = ?',
            [$usedAt, $token]
        );
        
        return [
            'file_id' => $presignedUrl['file_id'],
            'message' => 'Version uploaded successfully via presigned URL'
        ];
    }
    
    public function getPresignedUrls($fileId, $userId) {
        // Check if user can view this file
        if (!$this->canViewFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        return $this->db->fetchAll('
            SELECT id, token, created_at, expires_at, is_used, used_at, comment_prefix
            FROM presigned_upload_urls 
            WHERE file_id = ? 
            ORDER BY created_at DESC',
            [$fileId]
        );
    }
    
    public function revokePresignedUrl($fileId, $urlId, $userId) {
        // Check if user can edit this file
        if (!$this->canEditFile($fileId, $userId)) {
            throw new Exception('Permission denied');
        }
        
        $revokedAt = date('Y-m-d H:i:s');
        $this->db->execute('
            UPDATE presigned_upload_urls 
            SET is_used = TRUE, used_at = ? 
            WHERE id = ? AND file_id = ?',
            [$revokedAt, $urlId, $fileId]
        );
        
        return true;
    }
}