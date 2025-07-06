<?php
// Clear log file
$logFile = __DIR__ . '/error.log';

if (file_exists($logFile)) {
    file_put_contents($logFile, '');
    echo "Log file cleared: $logFile\n";
} else {
    echo "Log file not found: $logFile\n";
}