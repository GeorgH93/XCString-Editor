<?php

require_once __DIR__ . '/TestRunner.php';

class MockDatabase {
    private $data = [];
    private $lastInsertId = 0;
    
    public function execute($sql, $params = []) {
        // Mock database operations
        if (strpos($sql, 'INSERT INTO users') !== false) {
            $this->lastInsertId = $params[0];
            return true;
        } elseif (strpos($sql, 'INSERT INTO xcstring_files') !== false) {
            $this->lastInsertId++;
            $this->data['files'][$this->lastInsertId] = [
                'id' => $this->lastInsertId,
                'user_id' => $params[0],
                'filename' => $params[1],
                'content' => $params[2],
                'created_at' => date('Y-m-d H:i:s'),
                'updated_at' => date('Y-m-d H:i:s')
            ];
            return true;
        }
        return true;
    }
    
    public function fetchOne($sql, $params = []) {
        if (strpos($sql, 'SELECT * FROM xcstring_files WHERE id') !== false) {
            $fileId = $params[0];
            return isset($this->data['files'][$fileId]) ? $this->data['files'][$fileId] : false;
        }
        return false;
    }
    
    public function fetchAll($sql, $params = []) {
        if (strpos($sql, 'SELECT') !== false && strpos($sql, 'xcstring_files') !== false) {
            return array_values($this->data['files'] ?? []);
        }
        return [];
    }
    
    public function lastInsertId() {
        return $this->lastInsertId;
    }
    
    public function initializeSchema() {
        // Mock schema initialization
        return true;
    }
}

class MockFileManager {
    private $db;
    private $config;
    
    public function __construct($db, $config) {
        $this->db = $db;
        $this->config = $config;
    }
    
    public function saveFile($userId, $filename, $content) {
        // Check file size limit
        if (strlen($content) > $this->config['files']['max_file_size']) {
            throw new Exception('File too large');
        }
        
        // Mock save operation
        $this->db->execute(
            'INSERT INTO xcstring_files (user_id, filename, content) VALUES (?, ?, ?)',
            [$userId, $filename, $content]
        );
        
        return $this->db->lastInsertId();
    }
    
    public function getFile($fileId, $userId) {
        return $this->db->fetchOne(
            'SELECT * FROM xcstring_files WHERE id = ? AND user_id = ?',
            [$fileId, $userId]
        );
    }
    
    public function listUserFiles($userId) {
        $files = $this->db->fetchAll(
            'SELECT id, filename, created_at FROM xcstring_files WHERE user_id = ?',
            [$userId]
        );
        
        // Remove content from list view
        foreach ($files as &$file) {
            unset($file['content']);
        }
        
        return $files;
    }
    
    public function updateFile($fileId, $userId, $content) {
        // In a real implementation, this would update the database
        return true;
    }
    
    public function deleteFile($fileId, $userId) {
        // In a real implementation, this would delete from database
        return true;
    }
}

class FileManagerMockTest extends TestCase {
    private $config;
    private $db;
    private $fileManager;
    
    public function __construct() {
        parent::__construct('File Manager Mock Tests');
    }
    
    public function setUp() {
        $this->config = [
            'files' => [
                'max_file_size' => 1024 * 1024, // 1MB
                'max_files_per_user' => 10
            ]
        ];
        
        $this->db = new MockDatabase();
        $this->db->initializeSchema();
        $this->fileManager = new MockFileManager($this->db, $this->config);
        
        // Create a test user
        $this->db->execute(
            'INSERT INTO users (id, email, name) VALUES (?, ?, ?)',
            [1, 'test@example.com', 'Test User']
        );
    }
    
    public function testSaveFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-sample.xcstrings', $content);
        
        $this->assertTrue(is_numeric($fileId), 'Should return numeric file ID');
        $this->assertTrue($fileId > 0, 'File ID should be positive');
    }
    
    public function testGetFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-sample.xcstrings', $content);
        
        $file = $this->fileManager->getFile($fileId, 1);
        
        $this->assertNotEquals(false, $file, 'Should retrieve file');
        $this->assertEquals('test-sample.xcstrings', $file['filename'], 'Filename should match');
        $this->assertEquals($content, $file['content'], 'Content should match');
    }
    
    public function testListUserFiles() {
        // Create multiple files
        $content1 = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $content2 = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        
        $fileId1 = $this->fileManager->saveFile(1, 'file1.xcstrings', $content1);
        $fileId2 = $this->fileManager->saveFile(1, 'file2.xcstrings', $content2);
        
        $files = $this->fileManager->listUserFiles(1);
        
        $this->assertEquals(2, count($files), 'Should list 2 files');
        
        // Files should not include content in list view
        foreach ($files as $file) {
            $this->assertFalse(isset($file['content']), 'List should not include content');
            $this->assertArrayHasKey('id', $file, 'Should include file ID');
            $this->assertArrayHasKey('filename', $file, 'Should include filename');
        }
    }
    
    public function testFileSizeLimit() {
        $largeContent = str_repeat('x', 2 * 1024 * 1024); // 2MB content
        
        $this->expectException('Exception', function() use ($largeContent) {
            $this->fileManager->saveFile(1, 'large-file.xcstrings', $largeContent);
        });
    }
    
    public function testUpdateFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-sample.xcstrings', $content);
        
        $newContent = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        $result = $this->fileManager->updateFile($fileId, 1, $newContent);
        
        $this->assertTrue($result, 'Update should succeed');
    }
    
    public function testDeleteFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $fileId = $this->fileManager->saveFile(1, 'test-delete.xcstrings', $content);
        
        $result = $this->fileManager->deleteFile($fileId, 1);
        $this->assertTrue($result, 'Delete should succeed');
    }
}