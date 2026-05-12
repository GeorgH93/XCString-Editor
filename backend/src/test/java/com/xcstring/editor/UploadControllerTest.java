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
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_XCSTRINGS = "{\"sourceLanguage\":\"en\",\"strings\":{\"hello\":{\"localizations\":{\"en\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Hello\"}}}}},\"version\":\"1.0\"}";

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
    void testGetUploadInstructions() throws Exception {
        mockMvc.perform(get("/backend/index.php/upload/{token}", "sometoken123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Ready for upload"))
            .andExpect(jsonPath("$.instructions").exists());
    }

    @Test
    void testUploadWithInvalidToken() throws Exception {
        mockMvc.perform(put("/backend/index.php/upload/{token}", "invalidtoken")
                .contentType(MediaType.TEXT_PLAIN)
                .content(VALID_XCSTRINGS))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testUploadWithEmptyContent() throws Exception {
        mockMvc.perform(put("/backend/index.php/upload/{token}", "sometoken")
                .contentType(MediaType.TEXT_PLAIN)
                .content(""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testUploadWithInvalidXcstrings() throws Exception {
        mockMvc.perform(put("/backend/index.php/upload/{token}", "sometoken")
                .contentType(MediaType.TEXT_PLAIN)
                .content("{invalid json}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
