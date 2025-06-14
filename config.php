<?php
// XCString Editor Configuration

return [
    // Database configuration
    'database' => [
        'driver' => 'sqlite', // sqlite, mysql, postgres
        'host' => 'localhost',
        'port' => 3306,
        'database' => 'xcstring_editor',
        'username' => '',
        'password' => '',
        'sqlite_path' => __DIR__ . '/data/database.sqlite',
    ],
    
    // User registration settings
    'registration' => [
        'enabled' => true,
        'allowed_domains' => [], // Empty array = all domains allowed, ['example.com', 'company.com'] = only these domains
        'require_email_verification' => false, // Future feature
    ],
    
    // Session settings
    'session' => [
        'lifetime' => 7 * 24 * 60 * 60, // 7 days in seconds
        'cookie_name' => 'xcstring_session',
        'cookie_secure' => false, // Set to true for HTTPS
        'cookie_httponly' => true,
    ],
    
    // File storage settings
    'files' => [
        'max_file_size' => 10 * 1024 * 1024, // 10MB
        'max_files_per_user' => 100,
    ],
    
    // OAuth2 providers configuration
    'oauth2' => [
        'enabled' => false, // Set to true to enable OAuth2 authentication
        'base_url' => 'http://localhost:8080', // Your application's base URL
        'providers' => [
            'google' => [
                'enabled' => false,
                'client_id' => '',
                'client_secret' => '',
                'redirect_uri' => 'http://localhost:8080/backend/index.php/auth/oauth/google/callback',
            ],
            'github' => [
                'enabled' => false,
                'client_id' => '',
                'client_secret' => '',
                'redirect_uri' => 'http://localhost:8080/backend/index.php/auth/oauth/github/callback',
            ],
            'microsoft' => [
                'enabled' => false,
                'client_id' => '',
                'client_secret' => '',
                'redirect_uri' => 'http://localhost:8080/backend/index.php/auth/oauth/microsoft/callback',
                'tenant' => 'common', // 'common', 'organizations', 'consumers', or specific tenant ID
            ],
            'gitlab' => [
                'enabled' => false,
                'client_id' => '',
                'client_secret' => '',
                'redirect_uri' => 'http://localhost:8080/backend/index.php/auth/oauth/gitlab/callback',
                'instance_url' => 'https://gitlab.com', // For self-hosted GitLab instances
            ],
        ],
    ],
    
    // Application settings
    'app' => [
        'name' => 'XCString Editor',
        'debug' => false,
    ],
];