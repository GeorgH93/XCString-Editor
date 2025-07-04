<?php

require_once __DIR__ . '/TestRunner.php';
require_once __DIR__ . '/../backend/OAuth2Provider.php';

class OAuth2ProviderTest extends TestCase {
    
    public function __construct() {
        parent::__construct('OAuth2 Provider Tests');
    }
    
    public function testGoogleProviderCreation() {
        $config = [
            'client_id' => 'test-google-client-id',
            'client_secret' => 'test-google-secret',
            'redirect_uri' => 'http://localhost:8080/callback'
        ];
        
        $provider = OAuth2ProviderFactory::create('google', $config);
        $this->assertInstanceOf('GoogleOAuth2Provider', $provider, 'Should create Google provider');
        $this->assertEquals('Google', $provider->getProviderName(), 'Provider name should be Google');
    }
    
    public function testGitHubProviderCreation() {
        $config = [
            'client_id' => 'test-github-client-id',
            'client_secret' => 'test-github-secret',
            'redirect_uri' => 'http://localhost:8080/callback'
        ];
        
        $provider = OAuth2ProviderFactory::create('github', $config);
        $this->assertInstanceOf('GitHubOAuth2Provider', $provider, 'Should create GitHub provider');
        $this->assertEquals('GitHub', $provider->getProviderName(), 'Provider name should be GitHub');
    }
    
    public function testMicrosoftProviderCreation() {
        $config = [
            'client_id' => 'test-microsoft-client-id',
            'client_secret' => 'test-microsoft-secret',
            'redirect_uri' => 'http://localhost:8080/callback',
            'tenant' => 'common'
        ];
        
        $provider = OAuth2ProviderFactory::create('microsoft', $config);
        $this->assertInstanceOf('MicrosoftOAuth2Provider', $provider, 'Should create Microsoft provider');
        $this->assertEquals('Microsoft', $provider->getProviderName(), 'Provider name should be Microsoft');
    }
    
    public function testGitLabProviderCreation() {
        $config = [
            'client_id' => 'test-gitlab-client-id',
            'client_secret' => 'test-gitlab-secret',
            'redirect_uri' => 'http://localhost:8080/callback',
            'instance_url' => 'https://gitlab.example.com'
        ];
        
        $provider = OAuth2ProviderFactory::create('gitlab', $config);
        $this->assertInstanceOf('GitLabOAuth2Provider', $provider, 'Should create GitLab provider');
        $this->assertEquals('GitLab', $provider->getProviderName(), 'Provider name should be GitLab');
    }
    
    public function testCustomProviderCreation() {
        $config = [
            'client_id' => 'test-custom-client-id',
            'client_secret' => 'test-custom-secret',
            'redirect_uri' => 'http://localhost:8080/callback',
            'display_name' => 'Custom SSO',
            'authorize_url' => 'https://sso.example.com/oauth/authorize',
            'token_url' => 'https://sso.example.com/oauth/token',
            'user_info_url' => 'https://sso.example.com/oauth/userinfo',
            'scope' => 'openid email profile',
            'user_id_field' => 'sub',
            'user_name_field' => 'name',
            'user_email_field' => 'email',
            'user_avatar_field' => 'picture'
        ];
        
        $mainConfig = [
            'oauth2' => [
                'custom_providers' => [
                    'custom' => $config
                ]
            ]
        ];
        
        $provider = OAuth2ProviderFactory::create('custom', $config, $mainConfig);
        $this->assertInstanceOf('CustomOAuth2Provider', $provider, 'Should create Custom provider');
        $this->assertEquals('Custom SSO', $provider->getProviderName(), 'Provider name should be Custom SSO');
    }
    
    public function testUnsupportedProviderThrowsException() {
        $config = ['client_id' => 'test'];
        
        $this->expectException('Exception', function() use ($config) {
            OAuth2ProviderFactory::create('unsupported', $config);
        });
    }
    
    public function testGoogleAuthorizationURL() {
        $config = [
            'client_id' => 'test-google-client-id',
            'redirect_uri' => 'http://localhost:8080/callback'
        ];
        
        $provider = OAuth2ProviderFactory::create('google', $config);
        $authUrl = $provider->getAuthorizationUrl('test-state-123');
        
        $this->assertTrue(strpos($authUrl, 'accounts.google.com') !== false, 'Should use Google auth URL');
        $this->assertTrue(strpos($authUrl, 'client_id=test-google-client-id') !== false, 'Should include client ID');
        $this->assertTrue(strpos($authUrl, 'state=test-state-123') !== false, 'Should include state');
        $this->assertTrue(strpos($authUrl, 'scope=openid+email+profile') !== false, 'Should include default scope');
    }
    
    public function testGitHubAuthorizationURL() {
        $config = [
            'client_id' => 'test-github-client-id',
            'redirect_uri' => 'http://localhost:8080/callback'
        ];
        
        $provider = OAuth2ProviderFactory::create('github', $config);
        $authUrl = $provider->getAuthorizationUrl('test-state-456');
        
        $this->assertTrue(strpos($authUrl, 'github.com/login/oauth/authorize') !== false, 'Should use GitHub auth URL');
        $this->assertTrue(strpos($authUrl, 'client_id=test-github-client-id') !== false, 'Should include client ID');
        $this->assertTrue(strpos($authUrl, 'state=test-state-456') !== false, 'Should include state');
        $this->assertTrue(strpos($authUrl, 'scope=user%3Aemail') !== false, 'Should include GitHub scope');
    }
    
    public function testCustomProviderAuthorizationURL() {
        $config = [
            'client_id' => 'test-custom-client-id',
            'redirect_uri' => 'http://localhost:8080/callback',
            'authorize_url' => 'https://sso.example.com/oauth/authorize',
            'scope' => 'openid email profile',
            'additional_params' => ['prompt' => 'consent', 'access_type' => 'offline']
        ];
        
        $mainConfig = [
            'oauth2' => [
                'custom_providers' => [
                    'custom' => $config
                ]
            ]
        ];
        
        $provider = OAuth2ProviderFactory::create('custom', $config, $mainConfig);
        $authUrl = $provider->getAuthorizationUrl('test-state-789');
        
        $this->assertTrue(strpos($authUrl, 'sso.example.com/oauth/authorize') !== false, 'Should use custom auth URL');
        $this->assertTrue(strpos($authUrl, 'client_id=test-custom-client-id') !== false, 'Should include client ID');
        $this->assertTrue(strpos($authUrl, 'state=test-state-789') !== false, 'Should include state');
        $this->assertTrue(strpos($authUrl, 'prompt=consent') !== false, 'Should include additional param');
        $this->assertTrue(strpos($authUrl, 'access_type=offline') !== false, 'Should include additional param');
    }
    
    public function testGetAvailableProvidersBuiltIn() {
        $config = [
            'oauth2' => [
                'enabled' => true,
                'providers' => [
                    'google' => [
                        'enabled' => true,
                        'client_id' => 'google-client-id'
                    ],
                    'github' => [
                        'enabled' => false,
                        'client_id' => 'github-client-id'
                    ],
                    'microsoft' => [
                        'enabled' => true,
                        'client_id' => '' // Empty client ID should exclude
                    ]
                ],
                'custom_providers' => [],
                'env_custom_providers' => []
            ]
        ];
        
        $providers = OAuth2ProviderFactory::getAvailableProviders($config);
        
        $this->assertEquals(1, count($providers), 'Should have only one available provider');
        $this->assertArrayHasKey('google', $providers, 'Should include Google');
        $this->assertFalse(isset($providers['github']), 'Should not include disabled GitHub');
        $this->assertFalse(isset($providers['microsoft']), 'Should not include Microsoft with empty client ID');
        
        $this->assertEquals('Google', $providers['google']['display_name'], 'Google display name should be correct');
        $this->assertEquals(null, $providers['google']['icon_svg'], 'Built-in providers should not have icon_svg');
    }
    
    public function testGetAvailableProvidersCustom() {
        $config = [
            'oauth2' => [
                'enabled' => true,
                'providers' => [],
                'custom_providers' => [
                    'keycloak' => [
                        'enabled' => true,
                        'client_id' => 'keycloak-client-id',
                        'display_name' => 'Keycloak SSO',
                        'icon_svg' => '<svg>keycloak-icon</svg>'
                    ]
                ],
                'env_custom_providers' => [
                    'okta' => [
                        'enabled' => true,
                        'client_id' => 'okta-client-id',
                        'display_name' => 'Okta SSO',
                        'icon_svg' => '<svg>okta-icon</svg>'
                    ]
                ]
            ]
        ];
        
        $providers = OAuth2ProviderFactory::getAvailableProviders($config);
        
        $this->assertEquals(2, count($providers), 'Should have two custom providers');
        $this->assertArrayHasKey('keycloak', $providers, 'Should include Keycloak');
        $this->assertArrayHasKey('okta', $providers, 'Should include Okta');
        
        $this->assertEquals('Keycloak SSO', $providers['keycloak']['display_name'], 'Keycloak display name should be correct');
        $this->assertEquals('<svg>keycloak-icon</svg>', $providers['keycloak']['icon_svg'], 'Keycloak should have custom icon');
        
        $this->assertEquals('Okta SSO', $providers['okta']['display_name'], 'Okta display name should be correct');
        $this->assertEquals('<svg>okta-icon</svg>', $providers['okta']['icon_svg'], 'Okta should have custom icon');
    }
    
    public function testGetAvailableProvidersDisabled() {
        $config = [
            'oauth2' => [
                'enabled' => false,
                'providers' => [
                    'google' => [
                        'enabled' => true,
                        'client_id' => 'google-client-id'
                    ]
                ]
            ]
        ];
        
        $providers = OAuth2ProviderFactory::getAvailableProviders($config);
        
        $this->assertEquals(0, count($providers), 'Should have no providers when OAuth2 is disabled');
    }
    
    public function testMicrosoftTenantConfiguration() {
        $config = [
            'client_id' => 'test-microsoft-client-id',
            'redirect_uri' => 'http://localhost:8080/callback',
            'tenant' => 'custom-tenant-id'
        ];
        
        $provider = OAuth2ProviderFactory::create('microsoft', $config);
        $authUrl = $provider->getAuthorizationUrl('test-state');
        
        $this->assertTrue(strpos($authUrl, 'login.microsoftonline.com/custom-tenant-id/oauth2') !== false, 
                         'Should use custom tenant in URL');
    }
    
    public function testGitLabInstanceConfiguration() {
        $config = [
            'client_id' => 'test-gitlab-client-id',
            'redirect_uri' => 'http://localhost:8080/callback',
            'instance_url' => 'https://gitlab.mycompany.com'
        ];
        
        $provider = OAuth2ProviderFactory::create('gitlab', $config);
        $authUrl = $provider->getAuthorizationUrl('test-state');
        
        $this->assertTrue(strpos($authUrl, 'gitlab.mycompany.com/oauth/authorize') !== false, 
                         'Should use custom GitLab instance URL');
    }
}