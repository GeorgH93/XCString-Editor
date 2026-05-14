package com.xcstring.editor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class XCStringsServiceIntegrationTest {

    @Autowired
    private XCStringsService xcStringsService;

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    private String readFixture(String filename) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + filename)) {
            assertNotNull(is, "Fixture file not found: " + filename);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        return gson.fromJson(json, Map.class);
    }

    @Nested
    @DisplayName("Parse Tests")
    class ParseTests {

        @Test
        @DisplayName("parse sample.xcstrings - verify basic structure")
        void parseSample_verifyBasicStructure() throws IOException {
            String content = readFixture("sample.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            assertEquals("en", result.get("sourceLanguage"));
            assertEquals("1.0", result.get("version"));

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            assertNotNull(strings);
            assertTrue(strings.containsKey("test_key_with_empty_localizations"));
            assertTrue(strings.containsKey("test_key_with_some_localizations"));
        }

        @Test
        @DisplayName("parse sample.xcstrings - verify localizations content")
        void parseSample_verifyLocalizations() throws IOException {
            String content = readFixture("sample.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");

            Map<String, Object> withSome = (Map<String, Object>) strings.get("test_key_with_some_localizations");
            assertEquals("This string has some localizations", withSome.get("comment"));
            Map<String, Object> localizations = (Map<String, Object>) withSome.get("localizations");
            assertNotNull(localizations);
            assertTrue(localizations.containsKey("en"));

            Map<String, Object> enL10n = (Map<String, Object>) localizations.get("en");
            Map<String, Object> stringUnit = (Map<String, Object>) enL10n.get("stringUnit");
            assertEquals("translated", stringUnit.get("state"));
            assertEquals("Hello", stringUnit.get("value"));
        }

        @Test
        @DisplayName("parse sample-ai-demo.xcstrings - verify multiple languages and states")
        void parseAiDemo_verifyMultipleLanguagesAndStates() throws IOException {
            String content = readFixture("sample-ai-demo.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            assertEquals("en", result.get("sourceLanguage"));
            assertEquals("1.0", result.get("version"));

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            assertEquals(3, strings.size());
            assertTrue(strings.containsKey("welcome_message"));
            assertTrue(strings.containsKey("login_button"));
            assertTrue(strings.containsKey("error_network"));
        }

        @Test
        @DisplayName("parse sample-ai-demo.xcstrings - verify login_button has en, es, fr")
        void parseAiDemo_verifyLoginButtonLanguages() throws IOException {
            String content = readFixture("sample-ai-demo.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> loginBtn = (Map<String, Object>) strings.get("login_button");
            Map<String, Object> localizations = (Map<String, Object>) loginBtn.get("localizations");

            assertTrue(localizations.containsKey("en"));
            assertTrue(localizations.containsKey("es"));
            assertTrue(localizations.containsKey("fr"));

            Map<String, Object> enUnit = (Map<String, Object>) ((Map<String, Object>) localizations.get("en")).get("stringUnit");
            assertEquals("translated", enUnit.get("state"));
            assertEquals("Sign In", enUnit.get("value"));

            Map<String, Object> esUnit = (Map<String, Object>) ((Map<String, Object>) localizations.get("es")).get("stringUnit");
            assertEquals("new", esUnit.get("state"));
            assertEquals("", esUnit.get("value"));

            Map<String, Object> frUnit = (Map<String, Object>) ((Map<String, Object>) localizations.get("fr")).get("stringUnit");
            assertEquals("translated", frUnit.get("state"));
            assertEquals("Se connecter", frUnit.get("value"));
        }

        @Test
        @DisplayName("parse sample-ai-demo.xcstrings - verify needs_review state on error_network")
        void parseAiDemo_verifyNeedsReviewState() throws IOException {
            String content = readFixture("sample-ai-demo.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> errorNet = (Map<String, Object>) strings.get("error_network");
            Map<String, Object> localizations = (Map<String, Object>) errorNet.get("localizations");

            Map<String, Object> esUnit = (Map<String, Object>) ((Map<String, Object>) localizations.get("es")).get("stringUnit");
            assertEquals("needs_review", esUnit.get("state"));
            assertEquals("Error de red. Por favor verifica tu conexión a internet.", esUnit.get("value"));
        }

        @Test
        @DisplayName("parse test-variations.xcstrings - verify plural variations")
        void parseVariations_verifyPluralVariations() throws IOException {
            String content = readFixture("test-variations.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> minutesKey = (Map<String, Object>) strings.get("%d minutes");
            assertNotNull(minutesKey);
            assertEquals("Duration in minutes", minutesKey.get("comment"));

            Map<String, Object> localizations = (Map<String, Object>) minutesKey.get("localizations");
            assertTrue(localizations.containsKey("en"));
            assertTrue(localizations.containsKey("de"));

            Map<String, Object> enL10n = (Map<String, Object>) localizations.get("en");
            Map<String, Object> variations = (Map<String, Object>) enL10n.get("variations");
            Map<String, Object> plural = (Map<String, Object>) variations.get("plural");
            assertTrue(plural.containsKey("one"));
            assertTrue(plural.containsKey("other"));

            Map<String, Object> oneUnit = (Map<String, Object>) ((Map<String, Object>) plural.get("one")).get("stringUnit");
            assertEquals("translated", oneUnit.get("state"));
            assertEquals("%d minute", oneUnit.get("value"));

            Map<String, Object> otherUnit = (Map<String, Object>) ((Map<String, Object>) plural.get("other")).get("stringUnit");
            assertEquals("new", otherUnit.get("state"));
            assertEquals("%d minutes", otherUnit.get("value"));
        }

        @Test
        @DisplayName("parse test-variations.xcstrings - verify device variations")
        void parseVariations_verifyDeviceVariations() throws IOException {
            String content = readFixture("test-variations.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> itemsKey = (Map<String, Object>) strings.get("%@ items");
            assertNotNull(itemsKey);
            assertEquals("Number of items with device variations", itemsKey.get("comment"));

            Map<String, Object> localizations = (Map<String, Object>) itemsKey.get("localizations");
            Map<String, Object> enL10n = (Map<String, Object>) localizations.get("en");
            Map<String, Object> variations = (Map<String, Object>) enL10n.get("variations");
            Map<String, Object> device = (Map<String, Object>) variations.get("device");

            assertTrue(device.containsKey("iphone"));
            assertTrue(device.containsKey("ipad"));
            assertTrue(device.containsKey("other"));

            Map<String, Object> ipadUnit = (Map<String, Object>) ((Map<String, Object>) device.get("ipad")).get("stringUnit");
            assertEquals("translated", ipadUnit.get("state"));
            assertEquals("%@ items on iPad", ipadUnit.get("value"));
        }

        @Test
        @DisplayName("parse test-variations.xcstrings - verify simple key and all states")
        void parseVariations_verifySimpleKeyAndAllStates() throws IOException {
            String content = readFixture("test-variations.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            assertEquals(3, strings.size());

            Map<String, Object> simpleKey = (Map<String, Object>) strings.get("simple_key");
            Map<String, Object> localizations = (Map<String, Object>) simpleKey.get("localizations");
            Map<String, Object> enUnit = (Map<String, Object>) ((Map<String, Object>) localizations.get("en")).get("stringUnit");
            assertEquals("Hello World", enUnit.get("value"));

            Map<String, Object> deUnit = (Map<String, Object>) ((Map<String, Object>) localizations.get("de")).get("stringUnit");
            assertEquals("Hallo Welt", deUnit.get("value"));

            Set<String> allStates = collectAllStates(result);
            assertTrue(allStates.contains("translated"));
            assertTrue(allStates.contains("new"));
            assertTrue(allStates.contains("needs_review"));
        }

        @SuppressWarnings("unchecked")
        private Set<String> collectAllStates(Map<String, Object> data) {
            java.util.HashSet<String> states = new java.util.HashSet<>();
            Map<String, Object> strings = (Map<String, Object>) data.get("strings");
            for (Object stringEntry : strings.values()) {
                collectStatesRecursive((Map<String, Object>) stringEntry, states);
            }
            return states;
        }

        @SuppressWarnings("unchecked")
        private void collectStatesRecursive(Map<String, Object> map, Set<String> states) {
            if (map.containsKey("state")) {
                states.add((String) map.get("state"));
            }
            for (Object value : map.values()) {
                if (value instanceof Map) {
                    collectStatesRecursive((Map<String, Object>) value, states);
                }
            }
        }

        @Test
        @DisplayName("parse test-empty-localizations.xcstrings - empty localizations become empty maps")
        void parseEmptyLocalizations_emptyMapsNotNull() throws IOException {
            String content = readFixture("test-empty-localizations.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> emptyKey = (Map<String, Object>) strings.get("test_key_with_empty_localizations");

            Map<String, Object> localizations = (Map<String, Object>) emptyKey.get("localizations");
            assertNotNull(localizations, "Empty localizations should be a map, not null");
            assertTrue(localizations.isEmpty(), "Empty localizations should be an empty map");
        }

        @Test
        @DisplayName("parse test-empty-localizations.xcstrings - non-empty localizations still work")
        void parseEmptyLocalizations_nonEmptyStillWork() throws IOException {
            String content = readFixture("test-empty-localizations.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> someKey = (Map<String, Object>) strings.get("test_key_with_some_localizations");
            Map<String, Object> localizations = (Map<String, Object>) someKey.get("localizations");

            assertNotNull(localizations);
            assertFalse(localizations.isEmpty());
            assertTrue(localizations.containsKey("en"));
        }

        @Test
        @DisplayName("parse test-missing-localizations.xcstrings - missing localizations filled as empty maps")
        void parseMissingLocalizations_filledAsEmptyMaps() throws IOException {
            String content = readFixture("test-missing-localizations.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> missingKey = (Map<String, Object>) strings.get("test_key_with_missing_localizations");

            Map<String, Object> localizations = (Map<String, Object>) missingKey.get("localizations");
            assertNotNull(localizations, "Missing localizations should be filled with empty map, not null");
            assertTrue(localizations.isEmpty(), "Filled-in localizations should be empty");
        }

        @Test
        @DisplayName("parse test-missing-localizations.xcstrings - preserves other keys")
        void parseMissingLocalizations_preservesOtherKeys() throws IOException {
            String content = readFixture("test-missing-localizations.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            assertEquals("en", result.get("sourceLanguage"));
            assertEquals("1.0", result.get("version"));

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            assertTrue(strings.containsKey("test_key_with_missing_localizations"));
        }

        @Test
        @DisplayName("parse test-multiline.xcstrings - newline characters preserved in values")
        void parseMultiline_newlinesPreserved() throws IOException {
            String content = readFixture("test-multiline.xcstrings");
            Map<String, Object> result = xcStringsService.parse(content);

            Map<String, Object> strings = (Map<String, Object>) result.get("strings");
            Map<String, Object> multilineKey = (Map<String, Object>) strings.get("multiline_test");
            Map<String, Object> localizations = (Map<String, Object>) multilineKey.get("localizations");
            Map<String, Object> enL10n = (Map<String, Object>) localizations.get("en");
            Map<String, Object> stringUnit = (Map<String, Object>) enL10n.get("stringUnit");

            String value = (String) stringUnit.get("value");
            assertNotNull(value);
            assertTrue(value.contains("\n"), "Value should contain newline characters");
            assertTrue(value.contains("Line 1"), "Value should contain 'Line 1'");
            assertTrue(value.contains("Line 2"), "Value should contain 'Line 2'");
            assertTrue(value.contains("Line 3"), "Value should contain 'Line 3'");
        }

        @Test
        @DisplayName("parse throws RuntimeException for invalid JSON")
        void parse_invalidJson_throwsRuntimeException() {
            assertThrows(RuntimeException.class, () -> xcStringsService.parse("{invalid json}"));
        }

        @Test
        @DisplayName("parse throws RuntimeException for null content")
        void parse_nullContent_throwsRuntimeException() {
            assertThrows(RuntimeException.class, () -> xcStringsService.parse(null));
        }

        @Test
        @DisplayName("parse handles content without strings key")
        void parse_noStringsKey_returnsDataAsIs() throws IOException {
            String json = "{\"sourceLanguage\":\"en\",\"version\":\"1.0\"}";
            Map<String, Object> result = xcStringsService.parse(json);

            assertEquals("en", result.get("sourceLanguage"));
            assertEquals("1.0", result.get("version"));
            assertNull(result.get("strings"));
        }
    }

    @Nested
    @DisplayName("Generate Tests")
    class GenerateTests {

        @Test
        @DisplayName("generate produces valid JSON with correct structure")
        void generate_producesValidJson() throws IOException {
            String content = readFixture("sample.xcstrings");
            Map<String, Object> parsed = xcStringsService.parse(content);
            String generated = xcStringsService.generate(parsed);

            JsonElement element = JsonParser.parseString(generated);
            assertTrue(element.isJsonObject());

            JsonObject obj = element.getAsJsonObject();
            assertEquals("en", obj.get("sourceLanguage").getAsString());
            assertEquals("1.0", obj.get("version").getAsString());
            assertTrue(obj.has("strings"));
            assertTrue(obj.get("strings").isJsonObject());
        }

        @Test
        @DisplayName("generate produces valid JSON for all fixtures")
        void generate_allFixtures_produceValidJson() throws IOException {
            String[] fixtures = {
                "sample.xcstrings",
                "sample-ai-demo.xcstrings",
                "test-variations.xcstrings",
                "test-empty-localizations.xcstrings",
                "test-missing-localizations.xcstrings",
                "test-multiline.xcstrings"
            };

            for (String fixture : fixtures) {
                String content = readFixture(fixture);
                Map<String, Object> parsed = xcStringsService.parse(content);
                String generated = xcStringsService.generate(parsed);

                JsonElement element = JsonParser.parseString(generated);
                assertTrue(element.isJsonObject(),
                    "Generated output for " + fixture + " should be valid JSON object");

                JsonObject obj = element.getAsJsonObject();
                assertTrue(obj.has("sourceLanguage"),
                    fixture + " should have sourceLanguage");
                assertTrue(obj.has("version"),
                    fixture + " should have version");
                assertTrue(obj.has("strings"),
                    fixture + " should have strings");
            }
        }

        @Test
        @DisplayName("generate converts empty arrays to empty objects")
        void generate_emptyArraysBecomeObjects() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sourceLanguage", "en");
            Map<String, Object> strings = new LinkedHashMap<>();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("localizations", new ArrayList<>());
            strings.put("test_key", entry);
            data.put("strings", strings);
            data.put("version", "1.0");

            String generated = xcStringsService.generate(data);

            JsonObject obj = JsonParser.parseString(generated).getAsJsonObject();
            JsonObject genStrings = obj.getAsJsonObject("strings");
            JsonObject testKey = genStrings.getAsJsonObject("test_key");
            JsonObject localizations = testKey.getAsJsonObject("localizations");
            assertNotNull(localizations, "Empty arrays should become empty objects");
            assertTrue(localizations.entrySet().isEmpty(), "Converted object should be empty");
        }

        @Test
        @DisplayName("generate uses 2-space indentation")
        void generate_uses2SpaceIndentation() throws IOException {
            String content = readFixture("sample.xcstrings");
            Map<String, Object> parsed = xcStringsService.parse(content);
            String generated = xcStringsService.generate(parsed);

            String[] lines = generated.split("\n");
            boolean found2SpaceIndent = false;
            for (String line : lines) {
                if (line.startsWith("  ") && !line.startsWith("    ")) {
                    found2SpaceIndent = true;
                    break;
                }
            }
            assertTrue(found2SpaceIndent, "Generated output should use 2-space indentation");

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                int indent = line.length() - line.stripLeading().length();
                assertEquals(0, indent % 2,
                    "All indentation should be multiples of 2 spaces: '" + line + "'");
            }
        }

        @Test
        @DisplayName("generate uses colon spacing with spaces around colon")
        void generate_usesColonSpacing() throws IOException {
            String content = readFixture("sample.xcstrings");
            Map<String, Object> parsed = xcStringsService.parse(content);
            String generated = xcStringsService.generate(parsed);

            assertTrue(generated.contains("\" : "),
                "Generated output should use '\" : ' colon spacing");

            assertFalse(generated.contains("\":\""),
                "Generated output should not have colons without spaces");
        }

        @Test
        @DisplayName("generate preserves multiline values")
        void generate_preservesMultilineValues() throws IOException {
            String content = readFixture("test-multiline.xcstrings");
            Map<String, Object> parsed = xcStringsService.parse(content);
            String generated = xcStringsService.generate(parsed);

            JsonObject obj = JsonParser.parseString(generated).getAsJsonObject();
            JsonObject strings = obj.getAsJsonObject("strings");
            JsonObject multiline = strings.getAsJsonObject("multiline_test");
            JsonObject localizations = multiline.getAsJsonObject("localizations");
            JsonObject en = localizations.getAsJsonObject("en");
            JsonObject stringUnit = en.getAsJsonObject("stringUnit");
            String value = stringUnit.get("value").getAsString();

            assertTrue(value.contains("Line 1"), "Multiline value should contain Line 1");
            assertTrue(value.contains("Line 2"), "Multiline value should contain Line 2");
            assertTrue(value.contains("Line 3"), "Multiline value should contain Line 3");
        }

        @Test
        @DisplayName("generate handles null input by producing JsonNull")
        void generate_handlesNullInput() {
            String result = xcStringsService.generate(null);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Round-Trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("round-trip sample.xcstrings preserves structure")
        void roundTrip_sample() throws IOException {
            assertRoundTripStructuralFidelity("sample.xcstrings");
        }

        @Test
        @DisplayName("round-trip sample-ai-demo.xcstrings preserves structure")
        void roundTrip_aiDemo() throws IOException {
            assertRoundTripStructuralFidelity("sample-ai-demo.xcstrings");
        }

        @Test
        @DisplayName("round-trip test-variations.xcstrings preserves structure")
        void roundTrip_variations() throws IOException {
            assertRoundTripStructuralFidelity("test-variations.xcstrings");
        }

        @Test
        @DisplayName("round-trip test-empty-localizations.xcstrings preserves structure")
        void roundTrip_emptyLocalizations() throws IOException {
            assertRoundTripStructuralFidelity("test-empty-localizations.xcstrings");
        }

        @Test
        @DisplayName("round-trip test-missing-localizations.xcstrings preserves structure")
        void roundTrip_missingLocalizations() throws IOException {
            assertRoundTripStructuralFidelity("test-missing-localizations.xcstrings");
        }

        @Test
        @DisplayName("round-trip test-multiline.xcstrings preserves structure")
        void roundTrip_multiline() throws IOException {
            assertRoundTripStructuralFidelity("test-multiline.xcstrings");
        }

        @SuppressWarnings("unchecked")
        private void assertRoundTripStructuralFidelity(String fixture) throws IOException {
            String content = readFixture(fixture);
            Map<String, Object> firstParse = xcStringsService.parse(content);

            String generated = xcStringsService.generate(firstParse);
            Map<String, Object> secondParse = xcStringsService.parse(generated);

            assertEquals(firstParse.get("sourceLanguage"), secondParse.get("sourceLanguage"),
                fixture + ": sourceLanguage should survive round-trip");
            assertEquals(firstParse.get("version"), secondParse.get("version"),
                fixture + ": version should survive round-trip");

            Map<String, Object> firstStrings = (Map<String, Object>) firstParse.get("strings");
            Map<String, Object> secondStrings = (Map<String, Object>) secondParse.get("strings");

            assertNotNull(firstStrings);
            assertNotNull(secondStrings);
            assertEquals(firstStrings.keySet(), secondStrings.keySet(),
                fixture + ": string keys should survive round-trip");

            for (String key : firstStrings.keySet()) {
                Map<String, Object> firstEntry = (Map<String, Object>) firstStrings.get(key);
                Map<String, Object> secondEntry = (Map<String, Object>) secondStrings.get(key);

                Map<String, Object> firstL10n = (Map<String, Object>) firstEntry.get("localizations");
                Map<String, Object> secondL10n = (Map<String, Object>) secondEntry.get("localizations");

                assertNotNull(firstL10n, fixture + ": " + key + " should have localizations after first parse");
                assertNotNull(secondL10n, fixture + ": " + key + " should have localizations after round-trip");
                assertEquals(firstL10n.keySet(), secondL10n.keySet(),
                    fixture + ": " + key + " localization keys should survive round-trip");
            }
        }

        @Test
        @DisplayName("round-trip double parse produces identical results")
        @SuppressWarnings("unchecked")
        void roundTrip_doubleParseIdentical() throws IOException {
            String content = readFixture("sample.xcstrings");

            Map<String, Object> firstParse = xcStringsService.parse(content);
            String generated = xcStringsService.generate(firstParse);
            Map<String, Object> secondParse = xcStringsService.parse(generated);
            String regenerated = xcStringsService.generate(secondParse);

            Map<String, Object> thirdParse = xcStringsService.parse(regenerated);
            Map<String, Object> secondStrings = (Map<String, Object>) secondParse.get("strings");
            Map<String, Object> thirdStrings = (Map<String, Object>) thirdParse.get("strings");

            assertEquals(secondStrings.keySet(), thirdStrings.keySet(),
                "String keys should stabilize after first round-trip");
        }

        @Test
        @DisplayName("round-trip preserves variation structures")
        @SuppressWarnings("unchecked")
        void roundTrip_preservesVariationStructures() throws IOException {
            String content = readFixture("test-variations.xcstrings");
            Map<String, Object> firstParse = xcStringsService.parse(content);
            String generated = xcStringsService.generate(firstParse);
            Map<String, Object> secondParse = xcStringsService.parse(generated);

            Map<String, Object> firstStrings = (Map<String, Object>) firstParse.get("strings");
            Map<String, Object> secondStrings = (Map<String, Object>) secondParse.get("strings");

            Map<String, Object> firstMinutes = (Map<String, Object>) firstStrings.get("%d minutes");
            Map<String, Object> secondMinutes = (Map<String, Object>) secondStrings.get("%d minutes");

            Map<String, Object> firstL10n = (Map<String, Object>) firstMinutes.get("localizations");
            Map<String, Object> secondL10n = (Map<String, Object>) secondMinutes.get("localizations");

            Map<String, Object> firstEn = (Map<String, Object>) firstL10n.get("en");
            Map<String, Object> secondEn = (Map<String, Object>) secondL10n.get("en");

            Map<String, Object> firstVariations = (Map<String, Object>) firstEn.get("variations");
            Map<String, Object> secondVariations = (Map<String, Object>) secondEn.get("variations");

            Map<String, Object> firstPlural = (Map<String, Object>) firstVariations.get("plural");
            Map<String, Object> secondPlural = (Map<String, Object>) secondVariations.get("plural");

            assertEquals(firstPlural.keySet(), secondPlural.keySet(),
                "Plural variation keys should survive round-trip");
        }

        @Test
        @DisplayName("round-trip preserves comment fields")
        @SuppressWarnings("unchecked")
        void roundTrip_preservesComments() throws IOException {
            String content = readFixture("sample-ai-demo.xcstrings");
            Map<String, Object> firstParse = xcStringsService.parse(content);
            String generated = xcStringsService.generate(firstParse);
            Map<String, Object> secondParse = xcStringsService.parse(generated);

            Map<String, Object> firstStrings = (Map<String, Object>) firstParse.get("strings");
            Map<String, Object> secondStrings = (Map<String, Object>) secondParse.get("strings");

            for (String key : firstStrings.keySet()) {
                Map<String, Object> firstEntry = (Map<String, Object>) firstStrings.get(key);
                Map<String, Object> secondEntry = (Map<String, Object>) secondStrings.get(key);

                assertEquals(firstEntry.get("comment"), secondEntry.get("comment"),
                    key + ": comment should survive round-trip");
            }
        }
    }
}
