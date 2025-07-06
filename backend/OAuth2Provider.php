<?php

abstract class OAuth2Provider {
    protected $config;
    protected $providerName;
    protected $baseUrl;
    
    public function __construct($config, $providerName, $baseUrl) {
        $this->config = $config;
        $this->providerName = $providerName;
        $this->baseUrl = $baseUrl;
    }
    
    protected function getRedirectUri() {
        // For custom providers, check if redirect_uri is explicitly set in config
        if (isset($this->config['redirect_uri']) && !empty($this->config['redirect_uri'])) {
            return $this->config['redirect_uri'];
        }
        
        // Default redirect URI pattern
        return $this->baseUrl . '/backend/index.php/auth/oauth/' . $this->providerName . '/callback';
    }
    
    abstract public function getAuthorizationUrl($state = null);
    abstract public function getAccessToken($authorizationCode);
    abstract public function getUserInfo($accessToken);
    abstract public function getProviderName();
    
    protected function generateState() {
        return bin2hex(random_bytes(16));
    }
    
    protected function makeHttpRequest($url, $data = null, $headers = []) {
        $ch = curl_init();
        
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_HTTPHEADER => array_merge([
                'User-Agent: XCString-Editor/1.0',
                'Accept: application/json',
            ], $headers),
        ]);
        
        if ($data !== null) {
            curl_setopt($ch, CURLOPT_POST, true);
            if (is_array($data)) {
                curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($data));
                // Add Content-Type header for form data
                curl_setopt($ch, CURLOPT_HTTPHEADER, array_merge([
                    'User-Agent: XCString-Editor/1.0',
                    'Accept: application/json',
                    'Content-Type: application/x-www-form-urlencoded'
                ], $headers));
            } else {
                curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
            }
        }
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        
        curl_close($ch);
        
        if ($error) {
            throw new Exception("HTTP request failed: $error");
        }
        
        if ($httpCode >= 400) {
            throw new Exception("HTTP request failed with status $httpCode: $response");
        }
        
        return $response;
    }
    
    protected function parseJsonResponse($response) {
        $data = json_decode($response, true);
        if (!$data) {
            throw new Exception("Invalid JSON response: $response");
        }
        return $data;
    }
}

class GoogleOAuth2Provider extends OAuth2Provider {
    
    public function getAuthorizationUrl($state = null) {
        if (!$state) {
            $state = $this->generateState();
        }
        
        $params = [
            'client_id' => $this->config['client_id'],
            'redirect_uri' => $this->getRedirectUri(),
            'scope' => 'openid email profile',
            'response_type' => 'code',
            'state' => $state,
            'access_type' => 'offline',
            'prompt' => 'consent',
        ];
        
        return 'https://accounts.google.com/o/oauth2/v2/auth?' . http_build_query($params);
    }
    
    public function getAccessToken($authorizationCode) {
        $data = [
            'client_id' => $this->config['client_id'],
            'client_secret' => $this->config['client_secret'],
            'code' => $authorizationCode,
            'grant_type' => 'authorization_code',
            'redirect_uri' => $this->getRedirectUri(),
        ];
        
        $response = $this->makeHttpRequest(
            'https://oauth2.googleapis.com/token',
            $data
        );
        
        $tokenData = $this->parseJsonResponse($response);
        
        if (!isset($tokenData['access_token'])) {
            throw new Exception('Access token not found in response');
        }
        
        return $tokenData['access_token'];
    }
    
    public function getUserInfo($accessToken) {
        $response = $this->makeHttpRequest(
            'https://www.googleapis.com/oauth2/v2/userinfo',
            null,
            ["Authorization: Bearer $accessToken"]
        );
        
        $userData = $this->parseJsonResponse($response);
        
        return [
            'provider' => 'google',
            'provider_id' => $userData['id'],
            'email' => $userData['email'],
            'name' => $userData['name'],
            'avatar' => $userData['picture'] ?? null,
        ];
    }
    
    public function getProviderName() {
        return 'Google';
    }
}

class GitHubOAuth2Provider extends OAuth2Provider {
    
    public function getAuthorizationUrl($state = null) {
        if (!$state) {
            $state = $this->generateState();
        }
        
        $params = [
            'client_id' => $this->config['client_id'],
            'redirect_uri' => $this->getRedirectUri(),
            'scope' => 'user:email',
            'state' => $state,
        ];
        
        return 'https://github.com/login/oauth/authorize?' . http_build_query($params);
    }
    
    public function getAccessToken($authorizationCode) {
        $data = [
            'client_id' => $this->config['client_id'],
            'client_secret' => $this->config['client_secret'],
            'code' => $authorizationCode,
        ];
        
        $response = $this->makeHttpRequest(
            'https://github.com/login/oauth/access_token',
            $data,
            ['Accept: application/json']
        );
        
        $tokenData = $this->parseJsonResponse($response);
        
        if (!isset($tokenData['access_token'])) {
            throw new Exception('Access token not found in response');
        }
        
        return $tokenData['access_token'];
    }
    
    public function getUserInfo($accessToken) {
        // Get user basic info
        $userResponse = $this->makeHttpRequest(
            'https://api.github.com/user',
            null,
            ["Authorization: Bearer $accessToken"]
        );
        
        $userData = $this->parseJsonResponse($userResponse);
        
        // Get user email (might be private)
        $emailResponse = $this->makeHttpRequest(
            'https://api.github.com/user/emails',
            null,
            ["Authorization: Bearer $accessToken"]
        );
        
        $emailData = $this->parseJsonResponse($emailResponse);
        $primaryEmail = null;
        
        foreach ($emailData as $email) {
            if ($email['primary']) {
                $primaryEmail = $email['email'];
                break;
            }
        }
        
        if (!$primaryEmail && !empty($emailData)) {
            $primaryEmail = $emailData[0]['email'];
        }
        
        return [
            'provider' => 'github',
            'provider_id' => $userData['id'],
            'email' => $primaryEmail ?: $userData['email'],
            'name' => $userData['name'] ?: $userData['login'],
            'avatar' => $userData['avatar_url'] ?? null,
        ];
    }
    
    public function getProviderName() {
        return 'GitHub';
    }
}

class MicrosoftOAuth2Provider extends OAuth2Provider {
    
    public function getAuthorizationUrl($state = null) {
        if (!$state) {
            $state = $this->generateState();
        }
        
        $tenant = $this->config['tenant'] ?? 'common';
        
        $params = [
            'client_id' => $this->config['client_id'],
            'redirect_uri' => $this->getRedirectUri(),
            'scope' => 'openid email profile User.Read',
            'response_type' => 'code',
            'state' => $state,
        ];
        
        return "https://login.microsoftonline.com/$tenant/oauth2/v2.0/authorize?" . http_build_query($params);
    }
    
    public function getAccessToken($authorizationCode) {
        $tenant = $this->config['tenant'] ?? 'common';
        
        $data = [
            'client_id' => $this->config['client_id'],
            'client_secret' => $this->config['client_secret'],
            'code' => $authorizationCode,
            'grant_type' => 'authorization_code',
            'redirect_uri' => $this->getRedirectUri(),
        ];
        
        $response = $this->makeHttpRequest(
            "https://login.microsoftonline.com/$tenant/oauth2/v2.0/token",
            $data
        );
        
        $tokenData = $this->parseJsonResponse($response);
        
        if (!isset($tokenData['access_token'])) {
            throw new Exception('Access token not found in response');
        }
        
        return $tokenData['access_token'];
    }
    
    public function getUserInfo($accessToken) {
        $response = $this->makeHttpRequest(
            'https://graph.microsoft.com/v1.0/me',
            null,
            ["Authorization: Bearer $accessToken"]
        );
        
        $userData = $this->parseJsonResponse($response);
        
        return [
            'provider' => 'microsoft',
            'provider_id' => $userData['id'],
            'email' => $userData['mail'] ?: $userData['userPrincipalName'],
            'name' => $userData['displayName'],
            'avatar' => null, // Microsoft Graph requires separate call for photo
        ];
    }
    
    public function getProviderName() {
        return 'Microsoft';
    }
}

class GitLabOAuth2Provider extends OAuth2Provider {
    
    public function getAuthorizationUrl($state = null) {
        if (!$state) {
            $state = $this->generateState();
        }
        
        $instanceUrl = $this->config['instance_url'] ?? 'https://gitlab.com';
        
        $params = [
            'client_id' => $this->config['client_id'],
            'redirect_uri' => $this->getRedirectUri(),
            'scope' => 'read_user',
            'response_type' => 'code',
            'state' => $state,
        ];
        
        return "$instanceUrl/oauth/authorize?" . http_build_query($params);
    }
    
    public function getAccessToken($authorizationCode) {
        $instanceUrl = $this->config['instance_url'] ?? 'https://gitlab.com';
        
        $data = [
            'client_id' => $this->config['client_id'],
            'client_secret' => $this->config['client_secret'],
            'code' => $authorizationCode,
            'grant_type' => 'authorization_code',
            'redirect_uri' => $this->getRedirectUri(),
        ];
        
        $response = $this->makeHttpRequest(
            "$instanceUrl/oauth/token",
            $data
        );
        
        $tokenData = $this->parseJsonResponse($response);
        
        if (!isset($tokenData['access_token'])) {
            throw new Exception('Access token not found in response');
        }
        
        return $tokenData['access_token'];
    }
    
    public function getUserInfo($accessToken) {
        $instanceUrl = $this->config['instance_url'] ?? 'https://gitlab.com';
        
        $response = $this->makeHttpRequest(
            "$instanceUrl/api/v4/user",
            null,
            ["Authorization: Bearer $accessToken"]
        );
        
        $userData = $this->parseJsonResponse($response);
        
        return [
            'provider' => 'gitlab',
            'provider_id' => $userData['id'],
            'email' => $userData['email'],
            'name' => $userData['name'],
            'avatar' => $userData['avatar_url'] ?? null,
        ];
    }
    
    public function getProviderName() {
        return 'GitLab';
    }
}

class CustomOAuth2Provider extends OAuth2Provider {
    
    public function getAuthorizationUrl($state = null) {
        // Validate required configuration
        $requiredFields = ['client_id', 'authorize_url', 'scope'];
        foreach ($requiredFields as $field) {
            if (empty($this->config[$field])) {
                throw new Exception("Missing required configuration field for custom provider '{$this->providerName}': $field");
            }
        }
        
        if (!$state) {
            $state = $this->generateState();
        }
        
        $params = [
            'client_id' => $this->config['client_id'],
            'redirect_uri' => $this->getRedirectUri(),
            'scope' => $this->config['scope'],
            'response_type' => 'code',
            'state' => $state,
        ];
        
        // Add any additional parameters
        if (!empty($this->config['additional_params'])) {
            if (is_array($this->config['additional_params'])) {
                $params = array_merge($params, $this->config['additional_params']);
            } else if (is_string($this->config['additional_params'])) {
                // Handle JSON string format
                $additionalParams = json_decode($this->config['additional_params'], true);
                if ($additionalParams && is_array($additionalParams)) {
                    $params = array_merge($params, $additionalParams);
                }
            }
        }
        
        return $this->config['authorize_url'] . '?' . http_build_query($params);
    }
    
    public function getAccessToken($authorizationCode) {
        // Validate required configuration
        $requiredFields = ['client_id', 'client_secret', 'token_url'];
        foreach ($requiredFields as $field) {
            if (empty($this->config[$field])) {
                throw new Exception("Missing required configuration field for custom provider '{$this->providerName}': $field");
            }
        }
        
        $data = [
            'client_id' => $this->config['client_id'],
            'client_secret' => $this->config['client_secret'],
            'code' => $authorizationCode,
            'grant_type' => 'authorization_code',
            'redirect_uri' => $this->getRedirectUri(),
        ];
        
        $response = $this->makeHttpRequest(
            $this->config['token_url'],
            $data
        );
        
        $tokenData = $this->parseJsonResponse($response);
        
        if (!isset($tokenData['access_token'])) {
            throw new Exception("Access token not found in response from custom provider '{$this->providerName}'. Response: " . substr($response, 0, 500));
        }
        
        return $tokenData['access_token'];
    }
    
    public function getUserInfo($accessToken) {
        // Validate required configuration
        $requiredFields = ['user_info_url', 'user_id_field', 'user_email_field', 'user_name_field'];
        foreach ($requiredFields as $field) {
            if (empty($this->config[$field])) {
                throw new Exception("Missing required configuration field for custom provider '{$this->providerName}': $field");
            }
        }
        
        $response = $this->makeHttpRequest(
            $this->config['user_info_url'],
            null,
            ["Authorization: Bearer $accessToken"]
        );
        
        $userData = $this->parseJsonResponse($response);
        
        // Check if required fields exist in response
        $userIdField = $this->config['user_id_field'];
        $userEmailField = $this->config['user_email_field'];
        $userNameField = $this->config['user_name_field'];
        
        if (!isset($userData[$userIdField])) {
            throw new Exception("Field '{$userIdField}' not found in user info response for provider '{$this->providerName}'");
        }
        
        if (!isset($userData[$userEmailField])) {
            throw new Exception("Field '{$userEmailField}' not found in user info response for provider '{$this->providerName}'");
        }
        
        if (!isset($userData[$userNameField])) {
            throw new Exception("Field '{$userNameField}' not found in user info response for provider '{$this->providerName}'");
        }
        
        return [
            'provider' => $this->providerName,
            'provider_id' => $userData[$userIdField],
            'email' => $userData[$userEmailField],
            'name' => $userData[$userNameField],
            'avatar' => isset($this->config['user_avatar_field']) ? ($userData[$this->config['user_avatar_field']] ?? null) : null,
        ];
    }
    
    public function getProviderName() {
        return $this->config['display_name'] ?? ucfirst($this->providerName);
    }
}

class OAuth2ProviderFactory {
    public static function create($providerName, $config, $mainConfig = null) {
        $baseUrl = $mainConfig['app']['base_url'] ?? 'http://localhost:8080';
        
        switch ($providerName) {
            case 'google':
                return new GoogleOAuth2Provider($config, $providerName, $baseUrl);
            case 'github':
                return new GitHubOAuth2Provider($config, $providerName, $baseUrl);
            case 'microsoft':
                return new MicrosoftOAuth2Provider($config, $providerName, $baseUrl);
            case 'gitlab':
                return new GitLabOAuth2Provider($config, $providerName, $baseUrl);
            default:
                // Check if it's a custom provider
                if ($mainConfig && self::isCustomProvider($providerName, $mainConfig)) {
                    return new CustomOAuth2Provider($config, $providerName, $baseUrl);
                }
                throw new Exception("Unsupported OAuth2 provider: $providerName");
        }
    }
    
    private static function isCustomProvider($providerName, $mainConfig) {
        // Check if the provider is in custom_providers or env_custom_providers
        if (isset($mainConfig['oauth2']['custom_providers'][$providerName])) {
            return true;
        }
        
        if (isset($mainConfig['oauth2']['env_custom_providers'][$providerName])) {
            return true;
        }
        
        // Check if this is a custom provider that's not in the built-in providers list
        $builtInProviders = ['google', 'github', 'microsoft', 'gitlab'];
        return !in_array($providerName, $builtInProviders);
    }
    
    public static function getAvailableProviders($config) {
        $providers = [];
        
        if (!$config['oauth2']['enabled']) {
            return $providers;
        }
        
        // Built-in providers
        foreach ($config['oauth2']['providers'] as $name => $providerConfig) {
            if ($providerConfig['enabled'] && !empty($providerConfig['client_id'])) {
                $provider = self::create($name, $providerConfig, $config);
                $providers[$name] = [
                    'name' => $name,
                    'display_name' => $provider->getProviderName(),
                    'config' => $providerConfig,
                    'icon_svg' => null, // Built-in providers use hardcoded icons
                ];
            }
        }
        
        // Custom providers (manually configured)
        if (!empty($config['oauth2']['custom_providers'])) {
            foreach ($config['oauth2']['custom_providers'] as $name => $providerConfig) {
                if ($providerConfig['enabled'] && !empty($providerConfig['client_id'])) {
                    $provider = self::create($name, $providerConfig, $config);
                    $providers[$name] = [
                        'name' => $name,
                        'display_name' => $provider->getProviderName(),
                        'config' => $providerConfig,
                        'icon_svg' => $providerConfig['icon_svg'] ?? null,
                    ];
                }
            }
        }
        
        // Environment-based custom providers
        if (!empty($config['oauth2']['env_custom_providers'])) {
            foreach ($config['oauth2']['env_custom_providers'] as $name => $providerConfig) {
                if ($providerConfig['enabled'] && !empty($providerConfig['client_id'])) {
                    $provider = self::create($name, $providerConfig, $config);
                    $providers[$name] = [
                        'name' => $name,
                        'display_name' => $provider->getProviderName(),
                        'config' => $providerConfig,
                        'icon_svg' => $providerConfig['icon_svg'] ?? null,
                    ];
                }
            }
        }
        
        return $providers;
    }
}