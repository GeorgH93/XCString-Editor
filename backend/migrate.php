<?php
/**
 * Database Migration Runner
 * 
 * This script can be run manually to apply database migrations
 * or it's automatically called when the application starts
 */

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/Database.php';

try {
    $config = require __DIR__ . '/../config.php';
    
    echo "XCString Editor Database Migration Tool\n";
    echo "=====================================\n\n";
    
    // Initialize database connection
    $db = new Database($config);
    
    echo "Connected to database: " . $config['database']['driver'] . "\n";
    
    // Run migrations
    echo "Checking for pending migrations...\n";
    $db->runMigrations();
    
    echo "Migration check complete!\n";
    
} catch (Exception $e) {
    echo "Migration failed: " . $e->getMessage() . "\n";
    exit(1);
}