<?php

class FileManager {
    private $db;
    private $config;
    
    public function __construct($db, $config) {
        $this->db = $db;
        $this->config = $config;
    }
    
    public function saveFile($userId, $name, $content, $isPublic = false) {
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
        
        // Insert file
        $this->db->execute(
            'INSERT INTO xcstring_files (user_id, name, content, is_public, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)',
            [$userId, $name, $content, $isPublic ? 1 : 0]
        );
        
        return $this->db->lastInsertId();
    }
    
    public function updateFile($fileId, $userId, $content) {
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
        
        $this->db->execute(
            'UPDATE xcstring_files SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
            [$content, $fileId]
        );
        
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
}