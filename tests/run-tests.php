#!/usr/bin/env php
<?php
/**
 * XCString Editor Test Suite Runner
 * 
 * Usage: php tests/run-tests.php [test-name]
 * 
 * Examples:
 *   php tests/run-tests.php                    # Run all tests
 *   php tests/run-tests.php XCStringParsing    # Run specific test
 *   php tests/run-tests.php OAuth2Provider     # Run OAuth2 tests
 */

require_once __DIR__ . '/TestRunner.php';

// Change to project root directory
chdir(__DIR__ . '/..');

// Parse command line arguments
$specificTest = isset($argv[1]) ? $argv[1] : null;

// Initialize test runner
$runner = new TestRunner();

// Add all test cases
if (!$specificTest || strpos('XCStringParsing', $specificTest) !== false) {
    require_once __DIR__ . '/XCStringParsingTest.php';
    $runner->addTest(new XCStringParsingTest());
}

if (!$specificTest || strpos('XCStringExport', $specificTest) !== false) {
    require_once __DIR__ . '/XCStringExportTest.php';
    $runner->addTest(new XCStringExportTest());
}

if (!$specificTest || strpos('OAuth2Provider', $specificTest) !== false) {
    require_once __DIR__ . '/OAuth2ProviderTest.php';
    $runner->addTest(new OAuth2ProviderTest());
}

if (!$specificTest || strpos('FileManager', $specificTest) !== false) {
    // Use mock tests if SQLite is not available
    if (extension_loaded('pdo_sqlite')) {
        require_once __DIR__ . '/FileManagerTest.php';
        $runner->addTest(new FileManagerTest());
    } else {
        require_once __DIR__ . '/FileManagerMockTest.php';
        $runner->addTest(new FileManagerMockTest());
    }
}

// Run the tests
$runner->run();