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
    
    // AI translation and proofreading settings
    'ai' => [
        'enabled' => $_ENV['AI_ENABLED'] ?? false, // Set to true to enable AI features
        'providers' => [
            'openai' => [
                'enabled' => $_ENV['OPENAI_ENABLED'] ?? false,
                'api_key' => $_ENV['OPENAI_API_KEY'] ?? '',
                'base_url' => 'https://api.openai.com/v1', // Standard OpenAI API
                'models' => $_ENV['OPENAI_MODELS'] ? explode(',', $_ENV['OPENAI_MODELS']) : [
                    'gpt-4o',
                    'gpt-4o-mini',
                    'gpt-4-turbo',
                    'gpt-3.5-turbo'
                ],
            ],
            'anthropic' => [
                'enabled' => $_ENV['ANTHROPIC_ENABLED'] ?? false,
                'api_key' => $_ENV['ANTHROPIC_API_KEY'] ?? '',
                'base_url' => 'https://api.anthropic.com',
                'models' => $_ENV['ANTHROPIC_MODELS'] ? explode(',', $_ENV['ANTHROPIC_MODELS']) : [
                    'claude-3-5-sonnet-20241022',
                    'claude-3-5-haiku-20241022',
                    'claude-3-opus-20240229',
                    'claude-3-sonnet-20240229',
                    'claude-3-haiku-20240307'
                ],
            ],
            'openai_compatible' => [
                'enabled' => $_ENV['OPENAI_COMPATIBLE_ENABLED'] ?? false,
                'api_key' => $_ENV['OPENAI_COMPATIBLE_API_KEY'] ?? '',
                'base_url' => $_ENV['OPENAI_COMPATIBLE_BASE_URL'] ?? '', // e.g., 'https://api.groq.com/openai/v1'
                'models' => $_ENV['OPENAI_COMPATIBLE_MODELS'] ? explode(',', $_ENV['OPENAI_COMPATIBLE_MODELS']) : [
                    'llama-3.1-70b-versatile',
                    'llama-3.1-8b-instant',
                    'mixtral-8x7b-32768'
                ],
            ],
        ],
        'default_provider' => $_ENV['AI_DEFAULT_PROVIDER'] ?? 'openai',
        'default_model' => $_ENV['AI_DEFAULT_MODEL'] ?? 'gpt-4o-mini',
        'translation' => [
            'enabled' => $_ENV['AI_TRANSLATION_ENABLED'] ?? true,
            'max_context_strings' => 5, // Number of related strings to include as context
        ],
        'proofreading' => [
            'enabled' => $_ENV['AI_PROOFREADING_ENABLED'] ?? true,
        ],
    ],
    
    // Application settings
    'app' => [
        'name' => 'XCString Editor',
        'debug' => false,
    ],
];