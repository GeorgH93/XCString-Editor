package com.xcstring.editor.config;

import com.xcstring.editor.oauth2.OAuth2ProviderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "xcstring.oauth2.enabled=true",
        "xcstring.oauth2.custom-providers.authentik.enabled=true",
        "xcstring.oauth2.custom-providers.authentik.client-id=client_id",
        "xcstring.oauth2.custom-providers.authentik.client-secret=secret",
        "xcstring.oauth2.custom-providers.authentik.authorize-url=https://auth.example.com/authorize",
        "xcstring.oauth2.custom-providers.authentik.token-url=https://auth.example.com/token",
        "xcstring.oauth2.custom-providers.authentik.user-info-url=https://auth.example.com/userinfo"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomProviderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private OAuth2ProviderFactory oauth2ProviderFactory;

    @Test
    void customProviderIsBoundToAppProperties() {
        var customProviders = appProperties.getOauth2().getCustomProviders();
        var authentik = customProviders.get("authentik");

        assertNotNull(authentik, "authentik provider should be present in customProviders map");
        assertTrue(authentik.isEnabled(), "authentik should be enabled");
        assertEquals("client_id", authentik.getClientId());
        assertEquals("secret", authentik.getClientSecret());
        assertEquals("https://auth.example.com/authorize", authentik.getAuthorizeUrl());
        assertEquals("https://auth.example.com/token", authentik.getTokenUrl());
        assertEquals("https://auth.example.com/userinfo", authentik.getUserInfoUrl());
    }

    @Test
    void customProviderShowsUpInAvailableProviders() {
        var providers = oauth2ProviderFactory.getAvailableProviders(appProperties);

        assertTrue(providers.containsKey("authentik"),
                "authentik should be in available providers. Got: " + providers.keySet());
        assertEquals("Authentik", (String) providers.get("authentik").get("display_name"));
    }

    @Test
    void customProviderAppearsInAuthUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.oauth2_enabled").value(true))
                .andExpect(jsonPath("$.config.oauth2_providers", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.config.oauth2_providers[?(@.name=='authentik')]").exists())
                .andExpect(jsonPath("$.config.oauth2_providers[?(@.name=='authentik')].display_name").value("Authentik"));
    }

    private void assertNotNull(Object obj, String message) {
        org.junit.jupiter.api.Assertions.assertNotNull(obj, message);
    }

    private void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }

    private void assertEquals(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
