<?php

require_once 'TestRunner.php';
require_once __DIR__ . '/../backend/Database.php';
require_once __DIR__ . '/../backend/FileManager.php';

class FileVersionHistoryTest extends TestCase {
    private $config;
    private $db;
    private $fileManager;
    private $testUserId;
    private $testFileId;
    
    public function __construct() {
        parent::__construct('File Version History Tests');
    }
    
    public function setUp() {
        // Use in-memory SQLite for testing
        $this->config = [
            'database' => [
                'driver' => 'sqlite',
                'sqlite_path' => ':memory:'
            ],
            'files' => [
                'max_file_size' => 1024 * 1024, // 1MB
                'max_files_per_user' => 10
            ]
        ];
        
        try {
            $this->db = new Database($this->config);
            $this->db->initializeSchema();
            $this->fileManager = new FileManager($this->db, $this->config);
            
            // Create test user
            $this->db->execute(
                'INSERT INTO users (email, name) VALUES (?, ?)',
                ['test@example.com', 'Test User']
            );
            $this->testUserId = $this->db->lastInsertId();
            
        } catch (Exception $e) {
            throw new Exception("Setup failed: " . $e->getMessage());
        }
    }
    
    public function testCreateFileWithInitialVersion() {
        $content = '{"version": "1.0", "strings": {}}';
        $comment = 'Initial version';
        
        $fileId = $this->fileManager->saveFile(
            $this->testUserId, 
            'test.xcstrings', 
            $content, 
            false, 
            $comment
        );
        
        $this->assertNotNull($fileId);
        
        // Check that initial version was created
        $versions = $this->fileManager->getFileVersions($fileId, $this->testUserId);
        $this->assertEquals(1, count($versions));
        $this->assertEquals(1, $versions[0]['version_number']);
        $this->assertEquals($comment, $versions[0]['comment']);
        
        return $fileId;
    }
    
    public function testUpdateFileCreatesNewVersion() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        $newContent = '{"version": "1.1", "strings": {"hello": "world"}}';
        $comment = 'Added hello string';
        
        $this->fileManager->updateFile($fileId, $this->testUserId, $newContent, $comment);
        
        // Check that new version was created
        $versions = $this->fileManager->getFileVersions($fileId, $this->testUserId);
        $this->assertEquals(2, count($versions));
        $this->assertEquals(2, $versions[0]['version_number']); // Latest first
        $this->assertEquals($comment, $versions[0]['comment']);
    }
    
    public function testUpdateWithSameContentDoesNotCreateVersion() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        // Get current content
        $file = $this->fileManager->getFile($fileId, $this->testUserId);
        $originalContent = $file['content'];
        
        // Update with same content
        $this->fileManager->updateFile($fileId, $this->testUserId, $originalContent, 'No change');
        
        // Should still only have 1 version
        $versions = $this->fileManager->getFileVersions($fileId, $this->testUserId);
        $this->assertEquals(1, count($versions));
    }
    
    public function testGetSpecificVersion() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        // Add second version
        $newContent = '{"version": "1.1", "strings": {"hello": "world"}}';
        $this->fileManager->updateFile($fileId, $this->testUserId, $newContent, 'Version 2');
        
        // Get first version
        $version1 = $this->fileManager->getFileVersion($fileId, 1, $this->testUserId);
        $this->assertEquals(1, $version1['version_number']);
        $this->assertStringContains('"version": "1.0"', $version1['content']);
        
        // Get second version
        $version2 = $this->fileManager->getFileVersion($fileId, 2, $this->testUserId);
        $this->assertEquals(2, $version2['version_number']);
        $this->assertStringContains('"version": "1.1"', $version2['content']);
    }
    
    public function testRevertToVersion() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        // Add second version
        $secondContent = '{"version": "1.1", "strings": {"hello": "world"}}';
        $this->fileManager->updateFile($fileId, $this->testUserId, $secondContent, 'Version 2');
        
        // Add third version
        $thirdContent = '{"version": "1.2", "strings": {"hello": "world", "goodbye": "moon"}}';
        $this->fileManager->updateFile($fileId, $this->testUserId, $thirdContent, 'Version 3');
        
        // Revert to version 2
        $this->fileManager->revertToVersion($fileId, 2, $this->testUserId, 'Reverted to v2');
        
        // Check current content is from version 2
        $currentFile = $this->fileManager->getFile($fileId, $this->testUserId);
        $this->assertStringContains('"version": "1.1"', $currentFile['content']);
        $this->assertStringNotContains('goodbye', $currentFile['content']);
        
        // Should now have 4 versions (1, 2, 3, 4=revert)
        $versions = $this->fileManager->getFileVersions($fileId, $this->testUserId);
        $this->assertEquals(4, count($versions));
        $this->assertStringContains('Reverted to v2', $versions[0]['comment']);
    }
    
    public function testVersionStats() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        // Add more versions
        $this->fileManager->updateFile($fileId, $this->testUserId, '{"v": "2"}', 'Version 2');
        $this->fileManager->updateFile($fileId, $this->testUserId, '{"v": "3"}', 'Version 3');
        
        $stats = $this->fileManager->getFileVersionStats($fileId, $this->testUserId);
        
        $this->assertEquals(3, $stats['total_versions']);
        $this->assertEquals(1, $stats['unique_contributors']);
        $this->assertNotNull($stats['first_version_date']);
        $this->assertNotNull($stats['latest_version_date']);
        $this->assertGreaterThan(0, $stats['total_size_bytes']);
    }
    
    public function testDeleteVersion() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        // Add more versions
        $this->fileManager->updateFile($fileId, $this->testUserId, '{"v": "2"}', 'Version 2');
        $this->fileManager->updateFile($fileId, $this->testUserId, '{"v": "3"}', 'Version 3');
        
        // Delete version 2
        $this->fileManager->deleteFileVersion($fileId, 2, $this->testUserId);
        
        // Should now have 2 versions (1 and 3)
        $versions = $this->fileManager->getFileVersions($fileId, $this->testUserId);
        $this->assertEquals(2, count($versions));
        
        // Version 2 should not exist
        try {
            $this->fileManager->getFileVersion($fileId, 2, $this->testUserId);
            $this->fail('Should not be able to get deleted version');
        } catch (Exception $e) {
            $this->assertStringContains('Version not found', $e->getMessage());
        }
    }
    
    public function testCannotDeleteLatestVersion() {
        $fileId = $this->testCreateFileWithInitialVersion();
        $this->fileManager->updateFile($fileId, $this->testUserId, '{"v": "2"}', 'Version 2');
        
        try {
            $this->fileManager->deleteFileVersion($fileId, 2, $this->testUserId); // Latest version
            $this->fail('Should not be able to delete latest version');
        } catch (Exception $e) {
            $this->assertStringContains('Cannot delete the latest version', $e->getMessage());
        }
    }
    
    public function testCannotDeleteOnlyVersion() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        try {
            $this->fileManager->deleteFileVersion($fileId, 1, $this->testUserId); // Only version
            $this->fail('Should not be able to delete the only version');
        } catch (Exception $e) {
            $this->assertStringContains('Cannot delete the only version', $e->getMessage());
        }
    }
    
    public function testVersionPermissions() {
        $fileId = $this->testCreateFileWithInitialVersion();
        
        // Create another user
        $this->db->execute(
            'INSERT INTO users (email, name) VALUES (?, ?)',
            ['other@example.com', 'Other User']
        );
        $otherUserId = $this->db->lastInsertId();
        
        // Other user should not be able to access versions
        try {
            $this->fileManager->getFileVersions($fileId, $otherUserId);
            $this->fail('Should not be able to access versions without permission');
        } catch (Exception $e) {
            $this->assertStringContains('Permission denied', $e->getMessage());
        }
    }
}

// Run the tests
$test = new FileVersionHistoryTest();
$test->run();