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
}