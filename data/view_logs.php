<?php
// Simple log viewer for Docker setup
// Usage: php view_logs.php [lines] [level]

$logFile = __DIR__ . '/error.log';
$lines = isset($argv[1]) ? intval($argv[1]) : 50;
$filterLevel = isset($argv[2]) ? strtoupper($argv[2]) : null;

if (!file_exists($logFile)) {
    echo "Log file not found: $logFile\n";
    exit(1);
}

$content = file_get_contents($logFile);
if (empty($content)) {
    echo "Log file is empty\n";
    exit(0);
}

$logLines = explode("\n", trim($content));

// Filter by level if specified
if ($filterLevel) {
    $logLines = array_filter($logLines, function($line) use ($filterLevel) {
        return strpos($line, "[$filterLevel]") !== false;
    });
}

// Get last N lines
$logLines = array_slice($logLines, -$lines);

// Display with colors in terminal
foreach ($logLines as $line) {
    if (strpos($line, '[ERROR]') !== false) {
        echo "\033[31m$line\033[0m\n"; // Red
    } elseif (strpos($line, '[WARN]') !== false) {
        echo "\033[33m$line\033[0m\n"; // Yellow
    } elseif (strpos($line, '[INFO]') !== false) {
        echo "\033[32m$line\033[0m\n"; // Green
    } elseif (strpos($line, '[DEBUG]') !== false) {
        echo "\033[36m$line\033[0m\n"; // Cyan
    } else {
        echo "$line\n";
    }
}

echo "\n--- End of log (showing last $lines lines" . ($filterLevel ? " with level $filterLevel" : "") . ") ---\n";