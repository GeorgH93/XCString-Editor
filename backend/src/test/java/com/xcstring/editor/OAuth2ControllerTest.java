package com.xcstring.editor;

import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.oauth2.OAuth2Provider;
import com.xcstring.editor.oauth2.OAuth2ProviderFactory;
import com.xcstring.editor.oauth2.OAuth2UserInfo;
import com.xcstring.editor.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regression tests for the OAuth2 callback flow.
 *
 * Specifically guards against the bug where the OAuth2 callback redirected to
 * ?oauth_success=1 without creating a session cookie, leaving the user unauthenticated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AuthService authService;

    @MockBean
    private OAuth2ProviderFactory oauth2ProviderFactory;

    private static final String PROVIDER = "authentik";
    private static final String SESSION_COOKIE_NAME = "xcstring_session";

    @BeforeEach
    void setUp() {
        appProperties.getOauth2().setEnabled(true);
    }

    @Test
    void callback_newUser_setsSessionCookie() throws Exception {
        String state = authService.generateOAuth2State(PROVIDER);

        OAuth2Provider mockProvider = mock(OAuth2Provider.class);
        when(mockProvider.getAccessToken("valid-code")).thenReturn("mock-access-token");
        when(mockProvider.getUserInfo("mock-access-token")).thenReturn(new OAuth2UserInfo(
                PROVIDER,
                "external-id-123",
                "newuser@example.com",
                "New OAuth2 User",
                "https://example.com/avatar.png"
        ));
        when(oauth2ProviderFactory.create(eq(PROVIDER), any(AppProperties.class)))
                .thenReturn(mockProvider);

        MvcResult result = mockMvc.perform(get("/api/auth/oauth/" + PROVIDER + "/callback")
                        .param("code", "valid-code")
                        .param("state", state))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth_success=1")))
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertNotNull(sessionCookie, "Session cookie must be set after successful OAuth2 login");
        assertNotNull(sessionCookie.getValue(), "Session cookie value must not be null");
        assertFalse(sessionCookie.getValue().isEmpty(), "Session cookie value must not be empty");

        mockMvc.perform(get("/api/auth/user").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.user.name").value("New OAuth2 User"));
    }

    @Test
    void callback_existingUser_setsSessionCookie() throws Exception {
        // First, create the user via a prior OAuth2 flow
        String state1 = authService.generateOAuth2State(PROVIDER);
        OAuth2Provider mockProvider = mock(OAuth2Provider.class);
        when(mockProvider.getAccessToken(anyString())).thenReturn("token-1");
        when(mockProvider.getUserInfo("token-1")).thenReturn(new OAuth2UserInfo(
                PROVIDER, "ext-id-456", "existing@example.com", "Existing User", null));
        when(oauth2ProviderFactory.create(eq(PROVIDER), any(AppProperties.class)))
                .thenReturn(mockProvider);

        MvcResult first = mockMvc.perform(get("/api/auth/oauth/" + PROVIDER + "/callback")
                        .param("code", "code-1")
                        .param("state", state1))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Cookie firstCookie = first.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertNotNull(firstCookie, "First login should set a session cookie");

        // Now simulate a second login from the same user (returning user)
        String state2 = authService.generateOAuth2State(PROVIDER);
        when(mockProvider.getAccessToken(anyString())).thenReturn("token-2");
        when(mockProvider.getUserInfo("token-2")).thenReturn(new OAuth2UserInfo(
                PROVIDER, "ext-id-456", "existing@example.com", "Existing User Updated", null));

        MvcResult second = mockMvc.perform(get("/api/auth/oauth/" + PROVIDER + "/callback")
                        .param("code", "code-2")
                        .param("state", state2))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth_success=1")))
                .andReturn();

        Cookie secondCookie = second.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertNotNull(secondCookie, "Returning OAuth2 user must also receive a session cookie");
        assertNotEquals(firstCookie.getValue(), secondCookie.getValue(),
                "A new session should be issued on each OAuth2 login");
    }

    @Test
    void callback_invalidState_redirectsWithErrorAndNoSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/oauth/" + PROVIDER + "/callback")
                        .param("code", "any-code")
                        .param("state", "invalid-state-that-does-not-exist"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth_error")))
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertNull(sessionCookie, "No session cookie should be issued when state validation fails");
    }

    @Test
    void callback_oauth2Disabled_redirectsWithErrorAndNoSession() throws Exception {
        appProperties.getOauth2().setEnabled(false);

        MvcResult result = mockMvc.perform(get("/api/auth/oauth/" + PROVIDER + "/callback")
                        .param("code", "any-code")
                        .param("state", "any-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth_error")))
                .andReturn();

        assertNull(result.getResponse().getCookie(SESSION_COOKIE_NAME),
                "No session cookie should be issued when OAuth2 is disabled");
    }

    @Test
    void callback_existingEmailUser_withoutPriorOAuth2_setsSessionCookie() throws Exception {
        // Pre-register a user via email/password (no OAuth2 link yet)
        String registerBody = """
            {
                "email": "passworduser@example.com",
                "name": "Password User",
                "password": "password123"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        // Now log in via OAuth2 with the same email — exercises the "existing email, no OAuth2 link" branch
        String state = authService.generateOAuth2State(PROVIDER);
        OAuth2Provider mockProvider = mock(OAuth2Provider.class);
        when(mockProvider.getAccessToken("linking-code")).thenReturn("link-token");
        when(mockProvider.getUserInfo("link-token")).thenReturn(new OAuth2UserInfo(
                PROVIDER, "brand-new-ext-id-789", "passworduser@example.com",
                "Password User From OAuth", null));
        when(oauth2ProviderFactory.create(eq(PROVIDER), any(AppProperties.class)))
                .thenReturn(mockProvider);

        MvcResult result = mockMvc.perform(get("/api/auth/oauth/" + PROVIDER + "/callback")
                        .param("code", "linking-code")
                        .param("state", state))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("oauth_success=1")))
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertNotNull(sessionCookie, "Existing email user logging in via OAuth2 must receive a session cookie");

        // The session must authenticate as the existing user (not create a duplicate)
        mockMvc.perform(get("/api/auth/user").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("passworduser@example.com"));
    }
}
