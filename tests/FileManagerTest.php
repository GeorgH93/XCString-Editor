<?php

require_once __DIR__ . '/TestRunner.php';
require_once __DIR__ . '/../backend/Database.php';
require_once __DIR__ . '/../backend/FileManager.php';

class FileManagerTest extends TestCase {
    private $config;
    private $db;
    private $fileManager;
    private $tempDbPath;
    
    public function __construct() {
        parent::__construct('File Manager Tests');
    }
    
    public function setUp() {
        // Create a temporary SQLite database for testing
        $this->tempDbPath = tempnam(sys_get_temp_dir(), 'xcstring_test_') . '.sqlite';
        
        $this->config = [
            'database' => [
                'driver' => 'sqlite',
                'sqlite_path' => $this->tempDbPath
            ],
            'files' => [
                'max_file_size' => 1024 * 1024, // 1MB
                'max_files_per_user' => 10
            ]
        ];
        
        $this->db = new Database($this->config);
        $this->db->initializeSchema();
        $this->fileManager = new FileManager($this->db, $this->config);
        
        // Create a test user
        $this->db->execute(
            'INSERT INTO users (id, email, name) VALUES (?, ?, ?)',
            [1, 'test@example.com', 'Test User']
        );
    }
    
    public function tearDown() {
        if (file_exists($this->tempDbPath)) {
            unlink($this->tempDbPath);
        }
    }
    
    public function testSaveFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-sample.xcstrings', $content);
        
        $this->assertTrue(is_numeric($fileId), 'Should return numeric file ID');
        $this->assertTrue($fileId > 0, 'File ID should be positive');
        
        // Verify file was saved
        $file = $this->db->fetchOne(
            'SELECT * FROM xcstring_files WHERE id = ?',
            [$fileId]
        );
        
        $this->assertNotEquals(false, $file, 'File should exist in database');
        $this->assertEquals('test-sample.xcstrings', $file['filename'], 'Filename should match');
        $this->assertEquals(1, $file['user_id'], 'User ID should match');
        $this->assertEquals($content, $file['content'], 'Content should match');
    }
    
    public function testGetFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-sample.xcstrings', $content);
        
        $file = $this->fileManager->getFile($fileId, 1);
        
        $this->assertNotEquals(false, $file, 'Should retrieve file');
        $this->assertEquals('test-sample.xcstrings', $file['filename'], 'Filename should match');
        $this->assertEquals($content, $file['content'], 'Content should match');
        $this->assertArrayHasKey('created_at', $file, 'Should have creation timestamp');
        $this->assertArrayHasKey('updated_at', $file, 'Should have update timestamp');
    }
    
    public function testGetFileAccessControl() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'private-file.xcstrings', $content);
        
        // Try to access with different user ID
        $file = $this->fileManager->getFile($fileId, 999);
        $this->assertEquals(false, $file, 'Should not access other user\'s private file');
    }
    
    public function testUpdateFile() {
        $originalContent = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-sample.xcstrings', $originalContent);
        
        $newContent = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        $result = $this->fileManager->updateFile($fileId, 1, $newContent);
        
        $this->assertTrue($result, 'Update should succeed');
        
        $file = $this->fileManager->getFile($fileId, 1);
        $this->assertEquals($newContent, $file['content'], 'Content should be updated');
    }
    
    public function testDeleteFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-delete.xcstrings', $content);
        
        $result = $this->fileManager->deleteFile($fileId, 1);
        $this->assertTrue($result, 'Delete should succeed');
        
        $file = $this->fileManager->getFile($fileId, 1);
        $this->assertEquals(false, $file, 'File should no longer exist');
    }
    
    public function testListUserFiles() {
        // Create multiple files
        $content1 = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $content2 = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        
        $fileId1 = $this->fileManager->saveFile(1, 'file1.xcstrings', $content1);
        $fileId2 = $this->fileManager->saveFile(1, 'file2.xcstrings', $content2);
        
        $files = $this->fileManager->listUserFiles(1);
        
        $this->assertEquals(2, count($files), 'Should list 2 files');
        
        $filenames = array_column($files, 'filename');
        $this->assertTrue(in_array('file1.xcstrings', $filenames), 'Should include file1');
        $this->assertTrue(in_array('file2.xcstrings', $filenames), 'Should include file2');
        
        // Files should not include content in list view
        foreach ($files as $file) {
            $this->assertFalse(isset($file['content']), 'List should not include content');
            $this->assertArrayHasKey('id', $file, 'Should include file ID');
            $this->assertArrayHasKey('filename', $file, 'Should include filename');
            $this->assertArrayHasKey('created_at', $file, 'Should include creation time');
        }
    }
    
    public function testFileSizeLimit() {
        $largeContent = str_repeat('x', 2 * 1024 * 1024); // 2MB content
        
        $this->expectException('Exception', function() use ($largeContent) {
            $this->fileManager->saveFile(1, 'large-file.xcstrings', $largeContent);
        });
    }
    
    public function testMaxFilesPerUserLimit() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        
        // Create maximum allowed files
        for ($i = 1; $i <= 10; $i++) {
            $this->fileManager->saveFile(1, "file$i.xcstrings", $content);
        }
        
        // Try to create one more
        $this->expectException('Exception', function() use ($content) {
            $this->fileManager->saveFile(1, 'file11.xcstrings', $content);
        });
    }
    
    public function testShareFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'shared-file.xcstrings', $content);
        
        // Create another user
        $this->db->execute(
            'INSERT INTO users (id, email, name) VALUES (?, ?, ?)',
            [2, 'user2@example.com', 'User Two']
        );
        
        $result = $this->fileManager->shareFile($fileId, 1, 'user2@example.com', 'edit');
        $this->assertTrue($result, 'Share should succeed');
        
        // Verify share exists
        $share = $this->db->fetchOne(
            'SELECT * FROM file_shares WHERE file_id = ? AND shared_with_email = ?',
            [$fileId, 'user2@example.com']
        );
        
        $this->assertNotEquals(false, $share, 'Share should exist in database');
        $this->assertEquals('edit', $share['permission'], 'Permission should be edit');
    }
    
    public function testGetSharedFiles() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'shared-file.xcstrings', $content);
        
        // Create another user
        $this->db->execute(
            'INSERT INTO users (id, email, name) VALUES (?, ?, ?)',
            [2, 'user2@example.com', 'User Two']
        );
        
        $this->fileManager->shareFile($fileId, 1, 'user2@example.com', 'view');
        
        $sharedFiles = $this->fileManager->getSharedFiles('user2@example.com');
        
        $this->assertEquals(1, count($sharedFiles), 'Should have one shared file');
        $this->assertEquals('shared-file.xcstrings', $sharedFiles[0]['filename'], 'Filename should match');
        $this->assertEquals('view', $sharedFiles[0]['permission'], 'Permission should match');
        $this->assertEquals('test@example.com', $sharedFiles[0]['owner_email'], 'Owner should be correct');
    }
    
    public function testUnshareFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'shared-file.xcstrings', $content);
        
        // Create another user
        $this->db->execute(
            'INSERT INTO users (id, email, name) VALUES (?, ?, ?)',
            [2, 'user2@example.com', 'User Two']
        );
        
        $this->fileManager->shareFile($fileId, 1, 'user2@example.com', 'edit');
        $result = $this->fileManager->unshareFile($fileId, 1, 'user2@example.com');
        
        $this->assertTrue($result, 'Unshare should succeed');
        
        // Verify share is removed
        $share = $this->db->fetchOne(
            'SELECT * FROM file_shares WHERE file_id = ? AND shared_with_email = ?',
            [$fileId, 'user2@example.com']
        );
        
        $this->assertEquals(false, $share, 'Share should no longer exist');
    }
    
    public function testPublicFileAccess() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'public-file.xcstrings', $content);
        
        // Make file public
        $this->fileManager->shareFile($fileId, 1, '', 'public');
        
        // Try to access as another user
        $file = $this->fileManager->getFile($fileId, 999);
        $this->assertNotEquals(false, $file, 'Should access public file');
        $this->assertEquals('public-file.xcstrings', $file['filename'], 'Filename should match');
    }
    
    public function testGetFileShares() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'shared-file.xcstrings', $content);

        // Create test users
        $this->db->execute(
            'INSERT INTO users (id, email, name) VALUES (?, ?, ?)',
            [2, 'user2@example.com', 'User Two']
        );
        $this->db->execute(
            'INSERT INTO users (id, email, name) VALUES (?, ?, ?)',
            [3, 'user3@example.com', 'User Three']
        );

        $this->fileManager->shareFile($fileId, 1, 'user2@example.com', 'edit');
        $this->fileManager->shareFile($fileId, 1, 'user3@example.com', 'view');

        $shares = $this->fileManager->getFileShares($fileId, 1);

        $this->assertEquals(2, count($shares), 'Should have 2 shares');

        $emails = array_column($shares, 'shared_with_email');
        $this->assertTrue(in_array('user2@example.com', $emails), 'Should include user2');
        $this->assertTrue(in_array('user3@example.com', $emails), 'Should include user3');
    }

    public function testGeneratePresignedUploadUrl() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-file.xcstrings', $content);

        $result = $this->fileManager->generatePresignedUploadUrl($fileId, 1, 'CI Build');

        $this->assertArrayHasKey('token', $result, 'Should have token');
        $this->assertArrayHasKey('expires_at', $result, 'Should have expires_at');
        $this->assertArrayHasKey('upload_url', $result, 'Should have upload_url');
        $this->assertEquals(64, strlen($result['token']), 'Token should be 64 characters');

        // Verify URL is stored in database
        $presignedUrl = $this->db->fetchOne(
            'SELECT * FROM presigned_upload_urls WHERE token = ?',
            [$result['token']]
        );
        $this->assertNotEquals(false, $presignedUrl, 'URL should be stored in database');
        $this->assertEquals($fileId, $presignedUrl['file_id'], 'File ID should match');
        $this->assertEquals('CI Build', $presignedUrl['comment_prefix'], 'Comment prefix should match');
    }

    public function testUploadViaPresignedUrl() {
        $originalContent = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-file.xcstrings', $originalContent);

        $presignedResult = $this->fileManager->generatePresignedUploadUrl($fileId, 1, 'CI Upload');
        $token = $presignedResult['token'];

        $newContent = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        $result = $this->fileManager->uploadViaPresignedUrl($token, $newContent, 'Build #123');

        $this->assertArrayHasKey('file_id', $result, 'Should have file_id');
        $this->assertEquals($fileId, $result['file_id'], 'File ID should match');

        // Verify file was updated
        $file = $this->fileManager->getFile($fileId, 1);
        $this->assertEquals($newContent, $file['content'], 'Content should be updated');

        // Verify used_at was set
        $presignedUrl = $this->db->fetchOne(
            'SELECT * FROM presigned_upload_urls WHERE token = ?',
            [$token]
        );
        $this->assertNotEquals(false, $presignedUrl, 'URL should still exist');
        $this->assertTrue(!is_null($presignedUrl['used_at']), 'used_at should be set');
    }

    public function testUploadViaPresignedUrlIsReusable() {
        $originalContent = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-file.xcstrings', $originalContent);

        $presignedResult = $this->fileManager->generatePresignedUploadUrl($fileId, 1, 'CI Upload');
        $token = $presignedResult['token'];

        // First upload
        $newContent1 = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        $this->fileManager->uploadViaPresignedUrl($token, $newContent1, 'Build #1');

        // Second upload - should work since URLs are now reusable
        $newContent2 = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $result = $this->fileManager->uploadViaPresignedUrl($token, $newContent2, 'Build #2');

        $this->assertArrayHasKey('file_id', $result, 'Should have file_id');
        $this->assertEquals($fileId, $result['file_id'], 'File ID should match');

        // Verify file was updated again
        $file = $this->fileManager->getFile($fileId, 1);
        $this->assertEquals($newContent2, $file['content'], 'Content should be updated again');
    }

    public function testGetPresignedUrls() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-file.xcstrings', $content);

        $result1 = $this->fileManager->generatePresignedUploadUrl($fileId, 1, 'CI Build');
        $result2 = $this->fileManager->generatePresignedUploadUrl($fileId, 1, 'Manual Upload');

        $urls = $this->fileManager->getPresignedUrls($fileId, 1);

        $this->assertEquals(2, count($urls), 'Should have 2 URLs');
        $this->assertArrayHasKey('token', $urls[0], 'Should have token');
        $this->assertArrayHasKey('created_at', $urls[0], 'Should have created_at');
        $this->assertArrayHasKey('expires_at', $urls[0], 'Should have expires_at');
        $this->assertArrayHasKey('used_at', $urls[0], 'Should have used_at');
        $this->assertArrayHasKey('comment_prefix', $urls[0], 'Should have comment_prefix');
        $this->assertFalse(array_key_exists('is_used', $urls[0]), 'Should not have is_used field');
    }

    public function testRevokePresignedUrl() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-file.xcstrings', $content);

        $result = $this->fileManager->generatePresignedUploadUrl($fileId, 1, 'CI Build');
        $token = $result['token'];

        // Get the URL ID from database
        $presignedUrl = $this->db->fetchOne(
            'SELECT * FROM presigned_upload_urls WHERE token = ?',
            [$token]
        );
        $urlId = $presignedUrl['id'];

        $this->fileManager->revokePresignedUrl($fileId, $urlId, 1);

        // Verify URL was deleted
        $deletedUrl = $this->db->fetchOne(
            'SELECT * FROM presigned_upload_urls WHERE id = ?',
            [$urlId]
        );
        $this->assertEquals(false, $deletedUrl, 'URL should be deleted');
    }

    public function testUploadViaPresignedUrlExpired() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-file.xcstrings', $content);

        $presignedResult = $this->fileManager->generatePresignedUploadUrl($fileId, 1, 'CI Upload');
        $token = $presignedResult['token'];

        // Manually set expires_at to past
        $this->db->execute(
            'UPDATE presigned_upload_urls SET expires_at = ? WHERE token = ?',
            [date('Y-m-d H:i:s', strtotime('-1 day')), $token]
        );

        $newContent = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');

        $this->expectException('Exception', function() use ($token, $newContent) {
            $this->fileManager->uploadViaPresignedUrl($token, $newContent, 'Should fail');
        });
    }

    public function testUploadViaPresignedUrlInvalidToken() {
        $newContent = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');

        $this->expectException('Exception', function() use ($newContent) {
            $this->fileManager->uploadViaPresignedUrl('invalid_token_123', $newContent, 'Should fail');
        });
    }
}