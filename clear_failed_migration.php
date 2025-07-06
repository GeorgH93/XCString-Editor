<?php
// Clear failed migration to allow retry
require_once 'config.php';
require_once 'backend/Database.php';

try {
    $config = require 'config.php';
    $db = new Database($config);
    
    echo "Clearing failed migration entries...\n";
    
    // Remove the failed migration entry if it exists
    $db->execute("DELETE FROM migrations WHERE migration = ?", ['001_add_file_versions_table']);
    
    echo "Cleared migration entry for 001_add_file_versions_table\n";
    
    // Check if file_versions table exists and drop it if it does (partially created)
    try {
        $result = $db->fetchOne("SELECT name FROM sqlite_master WHERE type='table' AND name='file_versions'");
        if ($result) {
            $db->execute("DROP TABLE file_versions");
            echo "Dropped partially created file_versions table\n";
        }
    } catch (Exception $e) {
        echo "Note: Could not check/drop file_versions table (this is normal if it doesn't exist)\n";
    }
    
    echo "Ready to retry migration.\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}