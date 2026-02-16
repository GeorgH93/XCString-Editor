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
        'invite_domains' => !empty($_ENV['REGISTRATION_INVITE_DOMAINS']) ? explode(',', $_ENV['REGISTRATION_INVITE_DOMAINS']) : [], // Email domains that can create invites
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
        'providers' => [
            'google' => [
                'enabled' => filter_var($_ENV['OAUTH2_GOOGLE_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_GOOGLE_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_GOOGLE_CLIENT_SECRET'] ?? '',
            ],
            'github' => [
                'enabled' => filter_var($_ENV['OAUTH2_GITHUB_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_GITHUB_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_GITHUB_CLIENT_SECRET'] ?? '',
            ],
            'microsoft' => [
                'enabled' => filter_var($_ENV['OAUTH2_MICROSOFT_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_MICROSOFT_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_MICROSOFT_CLIENT_SECRET'] ?? '',
                'tenant' => $_ENV['OAUTH2_MICROSOFT_TENANT'] ?? 'common', // 'common', 'organizations', 'consumers', or specific tenant ID
            ],
            'gitlab' => [
                'enabled' => filter_var($_ENV['OAUTH2_GITLAB_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'client_id' => $_ENV['OAUTH2_GITLAB_CLIENT_ID'] ?? '',
                'client_secret' => $_ENV['OAUTH2_GITLAB_CLIENT_SECRET'] ?? '',
                'instance_url' => $_ENV['OAUTH2_GITLAB_INSTANCE_URL'] ?? 'https://gitlab.com', // For self-hosted GitLab instances
            ],
        ],
        // Custom OAuth2 providers - add unlimited custom providers here
        'custom_providers' => [
            // Example custom provider configuration
            // 'keycloak' => [
            //     'enabled' => filter_var($_ENV['OAUTH2_KEYCLOAK_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
            //     'display_name' => $_ENV['OAUTH2_KEYCLOAK_DISPLAY_NAME'] ?? 'Keycloak',
            //     'client_id' => $_ENV['OAUTH2_KEYCLOAK_CLIENT_ID'] ?? '',
            //     'client_secret' => $_ENV['OAUTH2_KEYCLOAK_CLIENT_SECRET'] ?? '',
            //     'redirect_uri' => $_ENV['OAUTH2_KEYCLOAK_REDIRECT_URI'] ?? ($_ENV['OAUTH2_BASE_URL'] ?? 'http://localhost:8080') . '/backend/index.php/auth/oauth/keycloak/callback',
            //     'authorize_url' => $_ENV['OAUTH2_KEYCLOAK_AUTHORIZE_URL'] ?? 'https://keycloak.example.com/auth/realms/master/protocol/openid-connect/auth',
            //     'token_url' => $_ENV['OAUTH2_KEYCLOAK_TOKEN_URL'] ?? 'https://keycloak.example.com/auth/realms/master/protocol/openid-connect/token',
            //     'user_info_url' => $_ENV['OAUTH2_KEYCLOAK_USER_INFO_URL'] ?? 'https://keycloak.example.com/auth/realms/master/protocol/openid-connect/userinfo',
            //     'scope' => $_ENV['OAUTH2_KEYCLOAK_SCOPE'] ?? 'openid email profile',
            //     'user_id_field' => $_ENV['OAUTH2_KEYCLOAK_USER_ID_FIELD'] ?? 'sub',
            //     'user_name_field' => $_ENV['OAUTH2_KEYCLOAK_USER_NAME_FIELD'] ?? 'name',
            //     'user_email_field' => $_ENV['OAUTH2_KEYCLOAK_USER_EMAIL_FIELD'] ?? 'email',
            //     'user_avatar_field' => $_ENV['OAUTH2_KEYCLOAK_USER_AVATAR_FIELD'] ?? 'picture',
            //     'icon_svg' => $_ENV['OAUTH2_KEYCLOAK_ICON_SVG'] ?? '<svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>',
            //     'additional_params' => [], // Extra parameters for authorization URL
            //     'allow_registration' => filter_var($_ENV['OAUTH2_KEYCLOAK_ALLOW_REGISTRATION'] ?? 'true', FILTER_VALIDATE_BOOLEAN), // Allow registration even if global registration is disabled
            // ],
        ],
        'env_custom_providers' => (function() {
            // Dynamic custom providers from environment variables
            // Format: OAUTH2_CUSTOM_PROVIDER_NAME_* where NAME is the provider key
            $customProviders = [];
            $baseUrl = $_ENV['APP_BASE_URL'] ?? 'http://localhost:8080';
            
            // Find all custom provider prefixes
            $providerPrefixes = [];
            foreach ($_ENV as $key => $value) {
                if (preg_match('/^OAUTH2_CUSTOM_PROVIDER_([A-Z_]+)_ENABLED$/', $key, $matches)) {
                    $providerName = strtolower($matches[1]);
                    if (filter_var($value, FILTER_VALIDATE_BOOLEAN)) {
                        $providerPrefixes[] = $providerName;
                    }
                }
            }
            
            // Build configuration for each custom provider
            foreach ($providerPrefixes as $providerName) {
                $providerKey = strtoupper($providerName);
                $customProviders[$providerName] = [
                    'enabled' => true,
                    'display_name' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_DISPLAY_NAME"] ?? ucfirst($providerName),
                    'client_id' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_CLIENT_ID"] ?? '',
                    'client_secret' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_CLIENT_SECRET"] ?? '',
                    'authorize_url' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_AUTHORIZE_URL"] ?? '',
                    'token_url' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_TOKEN_URL"] ?? '',
                    'user_info_url' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_USER_INFO_URL"] ?? '',
                    'scope' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_SCOPE"] ?? 'openid email profile',
                    'user_id_field' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_USER_ID_FIELD"] ?? 'sub',
                    'user_name_field' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_USER_NAME_FIELD"] ?? 'name',
                    'user_email_field' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_USER_EMAIL_FIELD"] ?? 'email',
                    'user_avatar_field' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_USER_AVATAR_FIELD"] ?? 'picture',
                    'icon_svg' => $_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_ICON_SVG"] ?? '<svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>',
                    'additional_params' => !empty($_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_ADDITIONAL_PARAMS"]) ? 
                        json_decode($_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_ADDITIONAL_PARAMS"], true) : [],
                    'allow_registration' => filter_var($_ENV["OAUTH2_CUSTOM_PROVIDER_{$providerKey}_ALLOW_REGISTRATION"] ?? 'true', FILTER_VALIDATE_BOOLEAN), // Allow registration even if global registration is disabled
                ];
            }
            
            return $customProviders;
        })(),
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
                    'gpt-5.2',
                    'gpt-5-mini',
                    'gpt-5.2-pro'
                ],
            ],
            'anthropic' => [
                'enabled' => filter_var($_ENV['ANTHROPIC_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'api_key' => $_ENV['ANTHROPIC_API_KEY'] ?? '',
                'base_url' => $_ENV['ANTHROPIC_BASE_URL'] ?? 'https://api.anthropic.com',
                'models' => !empty($_ENV['ANTHROPIC_MODELS']) ? explode(',', $_ENV['ANTHROPIC_MODELS']) : [
                    'claude-opus-4-6',
                    'claude-sonnet-4-5',
                    'claude-haiku-4-5'
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
            'zai' => [
                'enabled' => filter_var($_ENV['ZAI_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'api_key' => $_ENV['ZAI_API_KEY'] ?? '',
                'base_url' => $_ENV['ZAI_BASE_URL'] ?? 'https://api.z.ai/api/paas/v4',
                'models' => !empty($_ENV['ZAI_MODELS']) ? explode(',', $_ENV['ZAI_MODELS']) : [
                    'glm-5',
                    'glm-4.7'
                ],
            ],
            'deepl' => [
                'enabled' => filter_var($_ENV['DEEPL_ENABLED'] ?? 'false', FILTER_VALIDATE_BOOLEAN),
                'api_key' => $_ENV['DEEPL_API_KEY'] ?? '',
                'base_url' => $_ENV['DEEPL_BASE_URL'] ?? 'https://api.deepl.com',
                'models' => !empty($_ENV['DEEPL_MODELS']) ? explode(',', $_ENV['DEEPL_MODELS']) : [
                    'latency_optimized',
                    'quality_optimized',
                    'prefer_quality_optimized'
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
        'base_url' => $_ENV['APP_BASE_URL'] ?? 'http://localhost:8080', // Application's base URL
    ],
];
