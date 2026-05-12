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
class DebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/backend/index.php/auth/logout"));
    }

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

    @Test
    void testDebugInvitesNoAuth() throws Exception {
        mockMvc.perform(get("/backend/index.php/debug/invites"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.debug").exists())
            .andExpect(jsonPath("$.debug.invite_domains_configured").isBoolean())
            .andExpect(jsonPath("$.debug.registration_enabled").isBoolean())
            .andExpect(jsonPath("$.debug.current_user").isEmpty());
    }

    @Test
    void testDebugInvitesWithAuth() throws Exception {
        Cookie cookie = registerAndLogin("debug@example.com", "Debug User", "password123");

        mockMvc.perform(get("/backend/index.php/debug/invites")
                .cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.debug").exists())
            .andExpect(jsonPath("$.debug.current_user").exists())
            .andExpect(jsonPath("$.debug.current_user.id").isNumber())
            .andExpect(jsonPath("$.debug.current_user.email").value("debug@example.com"))
            .andExpect(jsonPath("$.debug.current_user.name").value("Debug User"))
            .andExpect(jsonPath("$.debug.user_domain").value("example.com"))
            .andExpect(jsonPath("$.debug.can_create_invites").isBoolean());
    }

    @Test
    void testTestEndpoint() throws Exception {
        mockMvc.perform(get("/backend/index.php/test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("XCString Tool API is working"));
    }
}
