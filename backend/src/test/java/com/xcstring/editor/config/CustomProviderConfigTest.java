package com.xcstring.editor.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomProviderConfigTest {

    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
    }

    @Test
    void discoversSingleCustomProvider() {
        Map<String, String> env = new HashMap<>();
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_ENABLED", "true");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_CLIENT_ID", "client_id");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_CLIENT_SECRET", "secret");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_AUTHORIZE_URL", "https://auth.example.com/authorize");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_TOKEN_URL", "https://auth.example.com/token");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_USER_INFO_URL", "https://auth.example.com/userinfo");

        registerWithEnv(env);

        var authentik = appProperties.getOauth2().getCustomProviders().get("authentik");
        assertNotNull(authentik);
        assertTrue(authentik.isEnabled());
        assertEquals("client_id", authentik.getClientId());
        assertEquals("secret", authentik.getClientSecret());
        assertEquals("https://auth.example.com/authorize", authentik.getAuthorizeUrl());
        assertEquals("https://auth.example.com/token", authentik.getTokenUrl());
        assertEquals("https://auth.example.com/userinfo", authentik.getUserInfoUrl());
    }

    @Test
    void discoversMultipleCustomProviders() {
        Map<String, String> env = new HashMap<>();
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_ENABLED", "true");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_CLIENT_ID", "authentik-id");
        env.put("OAUTH2_CUSTOM_PROVIDER_KEYCLOAK_ENABLED", "true");
        env.put("OAUTH2_CUSTOM_PROVIDER_KEYCLOAK_CLIENT_ID", "keycloak-id");
        env.put("OAUTH2_CUSTOM_PROVIDER_KEYCLOAK_CLIENT_SECRET", "keycloak-secret");

        registerWithEnv(env);

        var customProviders = appProperties.getOauth2().getCustomProviders();
        assertEquals("authentik-id", customProviders.get("authentik").getClientId());
        assertEquals("keycloak-id", customProviders.get("keycloak").getClientId());
        assertEquals("keycloak-secret", customProviders.get("keycloak").getClientSecret());
    }

    @Test
    void ignoresDisabledProviders() {
        Map<String, String> env = new HashMap<>();
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_ENABLED", "false");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_CLIENT_ID", "should-be-ignored");

        registerWithEnv(env);

        assertNull(appProperties.getOauth2().getCustomProviders().get("authentik"));
    }

    @Test
    void ignoresUnrelatedEnvVars() {
        Map<String, String> env = new HashMap<>();
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_ENABLED", "true");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_CLIENT_ID", "client_id");
        env.put("OAUTH2_GOOGLE_ENABLED", "true");
        env.put("SOME_OTHER_VAR", "value");

        registerWithEnv(env);

        var customProviders = appProperties.getOauth2().getCustomProviders();
        assertEquals(1, customProviders.size());
        assertEquals("client_id", customProviders.get("authentik").getClientId());
    }

    @Test
    void mapsAllFieldsCorrectly() {
        Map<String, String> env = new HashMap<>();
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_ENABLED", "true");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_DISPLAY_NAME", "My Provider");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_CLIENT_ID", "id");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_CLIENT_SECRET", "secret");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_AUTHORIZE_URL", "https://a");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_TOKEN_URL", "https://t");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_USER_INFO_URL", "https://u");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_SCOPE", "openid profile");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_USER_ID_FIELD", "sub");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_USER_NAME_FIELD", "name");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_USER_EMAIL_FIELD", "email");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_USER_AVATAR_FIELD", "picture");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_ICON_SVG", "<svg></svg>");
        env.put("OAUTH2_CUSTOM_PROVIDER_MYPROVIDER_ALLOW_REGISTRATION", "true");

        registerWithEnv(env);

        var props = appProperties.getOauth2().getCustomProviders().get("myprovider");
        assertEquals("My Provider", props.getDisplayName());
        assertEquals("id", props.getClientId());
        assertEquals("secret", props.getClientSecret());
        assertEquals("https://a", props.getAuthorizeUrl());
        assertEquals("https://t", props.getTokenUrl());
        assertEquals("https://u", props.getUserInfoUrl());
        assertEquals("openid profile", props.getScope());
        assertEquals("sub", props.getUserIdField());
        assertEquals("name", props.getUserNameField());
        assertEquals("email", props.getUserEmailField());
        assertEquals("picture", props.getUserAvatarField());
        assertEquals("<svg></svg>", props.getIconSvg());
        assertTrue(props.isAllowRegistration());
    }

    @Test
    void preservesDefaultsForUnsetFields() {
        Map<String, String> env = new HashMap<>();
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_ENABLED", "true");
        env.put("OAUTH2_CUSTOM_PROVIDER_AUTHENTIK_CLIENT_ID", "id");

        registerWithEnv(env);

        var props = appProperties.getOauth2().getCustomProviders().get("authentik");
        assertEquals("openid email profile", props.getScope());
        assertEquals("sub", props.getUserIdField());
        assertEquals("name", props.getUserNameField());
        assertEquals("email", props.getUserEmailField());
    }

    private void registerWithEnv(Map<String, String> env) {
        TestableCustomProviderConfig config = new TestableCustomProviderConfig(appProperties, env);
        config.registerCustomProviders();
    }

    private static class TestableCustomProviderConfig extends CustomProviderConfig {
        private final Map<String, String> env;

        TestableCustomProviderConfig(AppProperties appProperties, Map<String, String> env) {
            super(appProperties);
            this.env = env;
        }

        @Override
        Map<String, String> getEnv() {
            return env;
        }
    }
}
