<?php
// XCString Editor Configuration

return [
    // Database configuration
    'database' => [
        'driver' => $_ENV['DB_DRIVER'] ?? 'sqlite', // sqlite, mysql, postgres
        'host' => $_ENV['DB_HOST'] ?? 'localhost',
        'port' => (int)($_ENV['DB_PORT'] ?? 3306),
        'database' => $_ENV['DB_NAME'] ?? 'xcstring_editor',
        'username' => $_ENV['DB_USERNAME'] ?? '',
        'password' => $_ENV['DB_PASSWORD'] ?? '',
        'sqlite_path' => $_ENV['DB_SQLITE_PATH'] ?? __DIR__ . '/data/database.sqlite',
    ],
    
    // User registration settings
    'registration' => [
        'enabled' => filter_var($_ENV['REGISTRATION_ENABLED'] ?? 'true', FILTER_VALIDATE_BOOLEAN),
        'allowed_domains' => !empty($_ENV['REGISTRATION_ALLOWED_DOMAINS']) ? explode(',', $_ENV['REGISTRATION_ALLOWED_DOMAINS']) : [], // Empty array = all domains allowed
        'require_email_verification' => filter_var($_ENV['REGISTRATION_REQUIRE_EMAIL_VERIFICATION'] ?? 'false', FILTER_VALIDATE_BOOLEAN), // Future feature
    ],
    
    // Session settings
    'session' => [
        'lifetime' => (int)($_ENV['SESSION_LIFETIME'] ?? 7 * 24 * 60 * 60), // 7 days in seconds
        'cookie_name' => $_ENV['SESSION_COOKIE_NAME'] ?? 'xcstring_session',
        'cookie_secure' => filter_var($_ENV['SESSION_COOKIE_SECURE'] ?? 'false', FILTER_VALIDATE_BOOLEAN), // Set to true for HTTPS
        'cookie_httponly' => filter_var($_ENV['SESSION_COOKIE_HTTPONLY'] ?? 'true', FILTER_VALIDATE_BOOLEAN),
    ],
    
    // File storage settings
    'files' => [
        'max_file_size' => (int)($_ENV['FILES_MAX_FILE_SIZE'] ?? 10 * 1024 * 1024), // 10MB
        'max_files_per_user' => (int)($_ENV['FILES_MAX_FILES_PER_USER'] ?? 100),
    ],
    
    // OAuth2 providers configuration
    'oauth2' => [
        'enabled' => filter_var($_ENV['OAUTH2_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN), // Set to true to enable OAuth2 authentication
        'base_url' => $_ENV['OAUTH2_BASE_URL'] ?? 'http://localhost:8080', // Your application's base URL
        'providers' => [
            'google' => [
                'enabled' => filter_var($_ENV['OAUTH2_GOOGLE_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_GOOGLE_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_GOOGLE_CLIENT_SECRET'] ?? '',
                'redirect_uri' => $_ENV['OAUTH2_GOOGLE_REDIRECT_URI'] ?? ($_ENV['OAUTH2_BASE_URL'] ?? 'http://localhost:8080') . '/backend/index.php/auth/oauth/google/callback',
            ],
            'github' => [
                'enabled' => filter_var($_ENV['OAUTH2_GITHUB_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_GITHUB_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_GITHUB_CLIENT_SECRET'] ?? '',
                'redirect_uri' => $_ENV['OAUTH2_GITHUB_REDIRECT_URI'] ?? ($_ENV['OAUTH2_BASE_URL'] ?? 'http://localhost:8080') . '/backend/index.php/auth/oauth/github/callback',
            ],
            'microsoft' => [
                'enabled' => filter_var($_ENV['OAUTH2_MICROSOFT_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_MICROSOFT_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_MICROSOFT_CLIENT_SECRET'] ?? '',
                'redirect_uri' => $_ENV['OAUTH2_MICROSOFT_REDIRECT_URI'] ?? ($_ENV['OAUTH2_BASE_URL'] ?? 'http://localhost:8080') . '/backend/index.php/auth/oauth/microsoft/callback',
                'tenant' => $_ENV['OAUTH2_MICROSOFT_TENANT'] ?? 'common', // 'common', 'organizations', 'consumers', or specific tenant ID
            ],
            'gitlab' => [
                'enabled' => filter_var($_ENV['OAUTH2_GITLAB_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_GITLAB_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_GITLAB_CLIENT_SECRET'] ?? '',
                'redirect_uri' => $_ENV['OAUTH2_GITLAB_REDIRECT_URI'] ?? ($_ENV['OAUTH2_BASE_URL'] ?? 'http://localhost:8080') . '/backend/index.php/auth/oauth/gitlab/callback',
                'instance_url' => $_ENV['OAUTH2_GITLAB_INSTANCE_URL'] ?? 'https://gitlab.com', // For self-hosted GitLab instances
            ],
        ],
    ],
    
    // AI translation and proofreading settings
    'ai' => [
        'enabled' => filter_var($_ENV['AI_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN), // Set to true to enable AI features
        'providers' => [
            'openai' => [
                'enabled' => filter_var($_ENV['OPENAI_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'api_key' => $_ENV['OPENAI_API_KEY'] ?? '',
                'base_url' => $_ENV['OPENAI_BASE_URL'] ?? 'https://api.openai.com/v1', // Standard OpenAI API
                'models' => !empty($_ENV['OPENAI_MODELS']) ? explode(',', $_ENV['OPENAI_MODELS']) : [
                    'gpt-4o',
                    'gpt-4o-mini',
                    'gpt-4-turbo',
                    'gpt-3.5-turbo'
                ],
            ],
            'anthropic' => [
                'enabled' => filter_var($_ENV['ANTHROPIC_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'api_key' => $_ENV['ANTHROPIC_API_KEY'] ?? '',
                'base_url' => $_ENV['ANTHROPIC_BASE_URL'] ?? 'https://api.anthropic.com',
                'models' => !empty($_ENV['ANTHROPIC_MODELS']) ? explode(',', $_ENV['ANTHROPIC_MODELS']) : [
                    'claude-3-5-sonnet-20241022',
                    'claude-3-5-haiku-20241022',
                    'claude-3-opus-20240229',
                    'claude-3-sonnet-20240229',
                    'claude-3-haiku-20240307'
                ],
            ],
            'openai_compatible' => [
                'enabled' => filter_var($_ENV['OPENAI_COMPATIBLE_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'api_key' => $_ENV['OPENAI_COMPATIBLE_API_KEY'] ?? '',
                'base_url' => $_ENV['OPENAI_COMPATIBLE_BASE_URL'] ?? '', // e.g., 'https://api.groq.com/openai/v1'
                'models' => !empty($_ENV['OPENAI_COMPATIBLE_MODELS']) ? explode(',', $_ENV['OPENAI_COMPATIBLE_MODELS']) : [
                    'llama-3.1-70b-versatile',
                    'llama-3.1-8b-instant',
                    'mixtral-8x7b-32768'
                ],
            ],
        ],
        'default_provider' => $_ENV['AI_DEFAULT_PROVIDER'] ?? 'openai',
        'default_model' => $_ENV['AI_DEFAULT_MODEL'] ?? 'gpt-4o-mini',
        'translation' => [
            'enabled' => filter_var($_ENV['AI_TRANSLATION_ENABLED'] ?? 'true', FILTER_VALIDATE_BOOLEAN),
            'max_context_strings' => (int)($_ENV['AI_TRANSLATION_MAX_CONTEXT_STRINGS'] ?? 5), // Number of related strings to include as context
        ],
        'proofreading' => [
            'enabled' => filter_var($_ENV['AI_PROOFREADING_ENABLED'] ?? 'true', FILTER_VALIDATE_BOOLEAN),
        ],
    ],
    
    // Application settings
    'app' => [
        'name' => $_ENV['APP_NAME'] ?? 'XCString Editor',
        'debug' => filter_var($_ENV['APP_DEBUG'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
    ],
];