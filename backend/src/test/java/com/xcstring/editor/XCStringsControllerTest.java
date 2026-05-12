package com.xcstring.editor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XCStringsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VALID_XCSTRINGS = "{\"sourceLanguage\":\"en\",\"strings\":{\"hello\":{\"localizations\":{\"en\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Hello\"}}}}},\"version\":\"1.0\"}";

    @Test
    void testParseValidXcstrings() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of("content", VALID_XCSTRINGS));

        mockMvc.perform(post("/backend/index.php/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.sourceLanguage").value("en"))
            .andExpect(jsonPath("$.data.strings").exists())
            .andExpect(jsonPath("$.data.version").value("1.0"));
    }

    @Test
    void testParseInvalidJson() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of("content", "{invalid json}"));

        mockMvc.perform(post("/backend/index.php/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testParseMissingContent() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of("content", ""));

        mockMvc.perform(post("/backend/index.php/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testParseNoContentField() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/backend/index.php/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGenerateValidData() throws Exception {
        Map<String, Object> data = Map.of(
            "sourceLanguage", "en",
            "strings", Map.of(
                "hello", Map.of(
                    "localizations", Map.of(
                        "en", Map.of(
                            "stringUnit", Map.of(
                                "state", "translated",
                                "value", "Hello"
                            )
                        )
                    )
                )
            ),
            "version", "1.0"
        );
        String requestBody = objectMapper.writeValueAsString(Map.of("data", data));

        mockMvc.perform(post("/backend/index.php/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.xcstring").exists())
            .andExpect(jsonPath("$.xcstring").value(containsString("\"sourceLanguage\" : \"en\"")))
            .andExpect(jsonPath("$.xcstring").value(containsString("\"version\" : \"1.0\"")));
    }

    @Test
    void testGenerateMissingData() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/backend/index.php/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGenerateVerifyOutputFormat() throws Exception {
        Map<String, Object> data = Map.of(
            "sourceLanguage", "en",
            "strings", Map.of(
                "greeting", Map.of(
                    "localizations", Map.of(
                        "en", Map.of(
                            "stringUnit", Map.of(
                                "state", "translated",
                                "value", "Hello World"
                            )
                        )
                    )
                )
            ),
            "version", "1.0"
        );
        String requestBody = objectMapper.writeValueAsString(Map.of("data", data));

        mockMvc.perform(post("/backend/index.php/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.xcstring").exists())
            .andExpect(jsonPath("$.xcstring").value(containsString("  \"sourceLanguage\"")))
            .andExpect(jsonPath("$.xcstring").value(containsString("  \"strings\"")));
    }

    @Test
    void testParseWithMultipleLocalizations() throws Exception {
        String multiL10n = "{\"sourceLanguage\":\"en\",\"strings\":{\"welcome\":{\"localizations\":{\"en\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Welcome\"}},\"es\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Bienvenido\"}},\"fr\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Bienvenue\"}}}}},\"version\":\"1.0\"}";
        String requestBody = objectMapper.writeValueAsString(Map.of("content", multiL10n));

        mockMvc.perform(post("/backend/index.php/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.strings.welcome.localizations.en").exists())
            .andExpect(jsonPath("$.data.strings.welcome.localizations.es").exists())
            .andExpect(jsonPath("$.data.strings.welcome.localizations.fr").exists());
    }

    @Test
    void testParseWithComments() throws Exception {
        String withComments = "{\"sourceLanguage\":\"en\",\"strings\":{\"button.submit\":{\"comment\":\"Submit button text\",\"localizations\":{\"en\":{\"stringUnit\":{\"state\":\"translated\",\"value\":\"Submit\"}}}}},\"version\":\"1.0\"}";
        String requestBody = objectMapper.writeValueAsString(Map.of("content", withComments));

        mockMvc.perform(post("/backend/index.php/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.strings['button.submit'].comment").value("Submit button text"));
    }

    @Test
    void testRoundTrip() throws Exception {
        String parseRequest = objectMapper.writeValueAsString(Map.of("content", VALID_XCSTRINGS));

        String parseResponse = mockMvc.perform(post("/backend/index.php/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(parseRequest))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(parseResponse, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        
        String generateRequest = objectMapper.writeValueAsString(Map.of("data", data));

        mockMvc.perform(post("/backend/index.php/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(generateRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.xcstring").exists());
    }
}
