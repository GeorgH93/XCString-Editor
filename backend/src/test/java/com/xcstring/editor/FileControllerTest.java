package com.xcstring.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_XCSTRINGS = "{\"sourceLanguage\":\"en\",\"strings\":{\"hello\":{\"localizations\":{\"en\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Hello\"}}}}},\"version\":\"1.0\"}";

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/backend/index.php/auth/logout"));
    }

    private Cookie registerAndLogin(String email, String name, String password) throws Exception {
        Map<String, String> registerMap = new LinkedHashMap<>();
        registerMap.put("email", email);
        registerMap.put("name", name);
        registerMap.put("password", password);
        String registerBody = objectMapper.writeValueAsString(registerMap);

        mockMvc.perform(post("/backend/index.php/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        Map<String, String> loginMap = new LinkedHashMap<>();
        loginMap.put("email", email);
        loginMap.put("password", password);
        String loginBody = objectMapper.writeValueAsString(loginMap);

        MvcResult loginResult = mockMvc.perform(post("/backend/index.php/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        return loginResult.getResponse().getCookie("xcstring_session");
    }

    private Long createFile(Cookie cookie, String name, String content) throws Exception {
        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("name", name);
        requestMap.put("content", content);
        requestMap.put("is_public", false);
        String requestBody = objectMapper.writeValueAsString(requestMap);

        MvcResult result = mockMvc.perform(post("/backend/index.php/files/save")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("file_id").asLong();
    }

    @Test
    void testSaveFileAuthenticated() throws Exception {
        Cookie cookie = registerAndLogin("filetest1@example.com", "File Test 1", "password123");

        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("name", "test.xcstrings");
        requestMap.put("content", TEST_XCSTRINGS);
        requestMap.put("is_public", false);
        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/backend/index.php/files/save")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.file_id").isNumber());
    }

    @Test
    void testSaveFileNotAuthenticated() throws Exception {
        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("name", "test.xcstrings");
        requestMap.put("content", TEST_XCSTRINGS);
        requestMap.put("is_public", false);
        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/backend/index.php/files/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testSaveFileMissingName() throws Exception {
        Cookie cookie = registerAndLogin("filetest2@example.com", "File Test 2", "password123");

        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("name", "");
        requestMap.put("content", TEST_XCSTRINGS);
        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/backend/index.php/files/save")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testSaveFileMissingContent() throws Exception {
        Cookie cookie = registerAndLogin("filetest3@example.com", "File Test 3", "password123");

        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("name", "test.xcstrings");
        requestMap.put("content", "");
        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/backend/index.php/files/save")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testUpdateFile() throws Exception {
        Cookie cookie = registerAndLogin("filetest4@example.com", "File Test 4", "password123");
        Long fileId = createFile(cookie, "update-test.xcstrings", TEST_XCSTRINGS);

        String updatedContent = "{\"sourceLanguage\":\"en\",\"strings\":{\"hello\":{\"localizations\":{\"en\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Hello World\"}}}}},\"version\":\"1.0\"}";

        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("file_id", fileId);
        requestMap.put("content", updatedContent);
        requestMap.put("comment", "Updated greeting");
        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/backend/index.php/files/update")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetMyFiles() throws Exception {
        Cookie cookie = registerAndLogin("filetest5@example.com", "File Test 5", "password123");
        createFile(cookie, "my-file-1.xcstrings", TEST_XCSTRINGS);
        createFile(cookie, "my-file-2.xcstrings", TEST_XCSTRINGS);

        mockMvc.perform(get("/backend/index.php/files/my")
                .cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.files").isArray())
            .andExpect(jsonPath("$.files.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void testGetMyFilesNotAuthenticated() throws Exception {
        mockMvc.perform(get("/backend/index.php/files/my"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetPublicFiles() throws Exception {
        Cookie cookie = registerAndLogin("filetest6@example.com", "File Test 6", "password123");

        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("name", "public-file.xcstrings");
        requestMap.put("content", TEST_XCSTRINGS);
        requestMap.put("is_public", true);
        String requestBody = objectMapper.writeValueAsString(requestMap);

        mockMvc.perform(post("/backend/index.php/files/save")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/backend/index.php/files/public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.files").isArray());
    }

    @Test
    void testGetFileById() throws Exception {
        Cookie cookie = registerAndLogin("filetest7@example.com", "File Test 7", "password123");
        Long fileId = createFile(cookie, "get-by-id.xcstrings", TEST_XCSTRINGS);

        mockMvc.perform(get("/backend/index.php/files/{id}", fileId)
                .cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.file.id").value(fileId))
            .andExpect(jsonPath("$.file.name").value("get-by-id.xcstrings"))
            .andExpect(jsonPath("$.file.content").exists());
    }

    @Test
    void testGetFileByIdNonExistent() throws Exception {
        Cookie cookie = registerAndLogin("filetest8@example.com", "File Test 8", "password123");

        mockMvc.perform(get("/backend/index.php/files/{id}", 999999)
                .cookie(cookie))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testShareFile() throws Exception {
        Cookie ownerCookie = registerAndLogin("owner@example.com", "Owner User", "password123");
        Long fileId = createFile(ownerCookie, "shared-file.xcstrings", TEST_XCSTRINGS);

        registerAndLogin("sharetarget@example.com", "Share Target", "password123");

        Map<String, Object> shareMap = new LinkedHashMap<>();
        shareMap.put("file_id", fileId);
        shareMap.put("email", "sharetarget@example.com");
        shareMap.put("can_edit", false);
        String shareBody = objectMapper.writeValueAsString(shareMap);

        mockMvc.perform(post("/backend/index.php/files/share")
                .cookie(ownerCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(shareBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetFileShares() throws Exception {
        Cookie ownerCookie = registerAndLogin("owner2@example.com", "Owner 2", "password123");
        Long fileId = createFile(ownerCookie, "shared-file-2.xcstrings", TEST_XCSTRINGS);

        mockMvc.perform(get("/backend/index.php/files/{id}/shares", fileId)
                .cookie(ownerCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.shares").isArray())
            .andExpect(jsonPath("$.pending_shares").isArray());
    }

    @Test
    void testGetFileVersions() throws Exception {
        Cookie cookie = registerAndLogin("filetest9@example.com", "File Test 9", "password123");
        Long fileId = createFile(cookie, "versions-test.xcstrings", TEST_XCSTRINGS);

        String updatedContent = "{\"sourceLanguage\":\"en\",\"strings\":{\"hello\":{\"localizations\":{\"en\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Hi\"}}}}},\"version\":\"1.0\"}";

        Map<String, Object> updateMap = new LinkedHashMap<>();
        updateMap.put("file_id", fileId);
        updateMap.put("content", updatedContent);
        String updateBody = objectMapper.writeValueAsString(updateMap);

        mockMvc.perform(post("/backend/index.php/files/update")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/backend/index.php/files/{id}/versions", fileId)
                .cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.versions").isArray());
    }

    @Test
    void testGetFileVersionStats() throws Exception {
        Cookie cookie = registerAndLogin("filetest10@example.com", "File Test 10", "password123");
        Long fileId = createFile(cookie, "stats-test.xcstrings", TEST_XCSTRINGS);

        mockMvc.perform(get("/backend/index.php/files/{id}/version-stats", fileId)
                .cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.stats").exists());
    }

    @Test
    void testDeleteFile() throws Exception {
        Cookie cookie = registerAndLogin("filetest11@example.com", "File Test 11", "password123");
        Long fileId = createFile(cookie, "delete-test.xcstrings", TEST_XCSTRINGS);

        mockMvc.perform(delete("/backend/index.php/files/{id}", fileId)
                .cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/backend/index.php/files/{id}", fileId)
                .cookie(cookie))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testDeleteFileNotOwner() throws Exception {
        Cookie ownerCookie = registerAndLogin("owner3@example.com", "Owner 3", "password123");
        Long fileId = createFile(ownerCookie, "not-owner-delete.xcstrings", TEST_XCSTRINGS);

        Cookie otherCookie = registerAndLogin("other@example.com", "Other User", "password123");

        mockMvc.perform(delete("/backend/index.php/files/{id}", fileId)
                .cookie(otherCookie))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetSharedFiles() throws Exception {
        Cookie cookie = registerAndLogin("filetest12@example.com", "File Test 12", "password123");

        mockMvc.perform(get("/backend/index.php/files/shared")
                .cookie(cookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.files").isArray());
    }
}
