package com.xcstring.editor;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up - try to logout any existing session
        mockMvc.perform(post("/backend/index.php/auth/logout"));
    }

    @Test
    void testRegisterSuccess() throws Exception {
        String requestBody = """
            {
                "email": "test@example.com",
                "name": "Test User",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.user_id").isNumber());
    }

    @Test
    void testRegisterMissingFields() throws Exception {
        String requestBody = """
            {
                "email": "",
                "name": "",
                "password": ""
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterMissingEmail() throws Exception {
        String requestBody = """
            {
                "name": "Test User",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterDuplicateEmail() throws Exception {
        // First registration
        String requestBody = """
            {
                "email": "duplicate@example.com",
                "name": "First User",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        // Second registration with same email
        String requestBody2 = """
            {
                "email": "duplicate@example.com",
                "name": "Second User",
                "password": "password456"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Register first
        String registerBody = """
            {
                "email": "login@example.com",
                "name": "Login User",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        // Login
        String loginBody = """
            {
                "email": "login@example.com",
                "password": "password123"
            }
            """;

        MvcResult result = mockMvc.perform(post("/backend/index.php/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.user.id").isNumber())
            .andExpect(jsonPath("$.user.email").value("login@example.com"))
            .andExpect(jsonPath("$.user.name").value("Login User"))
            .andReturn();

        // Verify cookie is set
        jakarta.servlet.http.Cookie sessionCookie = result.getResponse().getCookie("xcstring_session");
        assert sessionCookie != null : "Session cookie should be set";
    }

    @Test
    void testLoginWrongPassword() throws Exception {
        // Register first
        String registerBody = """
            {
                "email": "wrongpass@example.com",
                "name": "Wrong Pass User",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        // Login with wrong password
        String loginBody = """
            {
                "email": "wrongpass@example.com",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLoginMissingFields() throws Exception {
        String loginBody = """
            {
                "email": "",
                "password": ""
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLogout() throws Exception {
        // Register and login first
        String registerBody = """
            {
                "email": "logout@example.com",
                "name": "Logout User",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        String loginBody = """
            {
                "email": "logout@example.com",
                "password": "password123"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/backend/index.php/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("xcstring_session");

        // Logout
        mockMvc.perform(post("/backend/index.php/auth/logout")
                .cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetUserNoAuth() throws Exception {
        mockMvc.perform(get("/backend/index.php/auth/user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.user").isEmpty())
            .andExpect(jsonPath("$.config").exists())
            .andExpect(jsonPath("$.config.registration_enabled").value(true))
            .andExpect(jsonPath("$.config.oauth2_enabled").value(false))
            .andExpect(jsonPath("$.config.ai_enabled").value(false));
    }

    @Test
    void testGetUserWithValidSession() throws Exception {
        // Register and login first
        String registerBody = """
            {
                "email": "getuser@example.com",
                "name": "Get User",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        String loginBody = """
            {
                "email": "getuser@example.com",
                "password": "password123"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/backend/index.php/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("xcstring_session");

        // Get user info
        mockMvc.perform(get("/backend/index.php/auth/user")
                .cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.user.id").isNumber())
            .andExpect(jsonPath("$.user.email").value("getuser@example.com"))
            .andExpect(jsonPath("$.user.name").value("Get User"))
            .andExpect(jsonPath("$.config").exists());
    }

    @Test
    void testValidateInviteTokenInvalid() throws Exception {
        mockMvc.perform(get("/backend/index.php/auth/invites/validate/invalidtoken123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void testValidateInviteTokenWithEmail() throws Exception {
        mockMvc.perform(get("/backend/index.php/auth/invites/validate/invalidtoken123")
                .param("email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void testTestEndpoint() throws Exception {
        mockMvc.perform(get("/backend/index.php/test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("XCString Tool API is working"));
    }

    // Helper method to register and login a user, returning the session cookie
    private Cookie registerAndLogin(String email, String name, String password) throws Exception {
        String registerBody = String.format("""
            {
                "email": "%s",
                "name": "%s",
                "password": "%s"
            }
            """, email, name, password);

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        String loginBody = String.format("""
            {
                "email": "%s",
                "password": "%s"
            }
            """, email, password);

        MvcResult loginResult = mockMvc.perform(post("/backend/index.php/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        return loginResult.getResponse().getCookie("xcstring_session");
    }
}
