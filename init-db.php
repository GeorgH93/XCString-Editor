<?php
// Database initialization script for Docker container startup

// Load configuration and classes
$config = require 'config.php';
require_once 'backend/Database.php';

try {
    echo "Initializing database...\n";
    
    $db = new Database($config);
    
    // Check if database needs initialization
    $needsInit = false;
    
    if ($config['database']['driver'] === 'sqlite') {
        $sqlitePath = $config['database']['sqlite_path'];
        if (!file_exists($sqlitePath)) {
            $needsInit = true;
            echo "SQLite database file not found, creating new database...\n";
        } else {
            // Check if tables exist
            try {
                $result = $db->query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'");
                if (!$result->fetch()) {
                    $needsInit = true;
                    echo "Database exists but tables missing, initializing schema...\n";
                }
            } catch (Exception $e) {
                $needsInit = true;
                echo "Database exists but schema check failed, reinitializing...\n";
            }
        }
    } else {
        // For MySQL/PostgreSQL, check if tables exist
        try {
            $result = $db->query("SELECT 1 FROM users LIMIT 1");
            echo "Database tables already exist, skipping initialization.\n";
        } catch (Exception $e) {
            $needsInit = true;
            echo "Database tables not found, initializing schema...\n";
        }
    }
    
    if ($needsInit) {
        $db->initializeSchema();
        echo "Database initialized successfully!\n";
    }
    
    echo "Database ready.\n";
    
} catch (Exception $e) {
    echo "Database initialization failed: " . $e->getMessage() . "\n";
    exit(1);
}