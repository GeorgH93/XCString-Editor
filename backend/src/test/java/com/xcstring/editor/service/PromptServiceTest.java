package com.xcstring.editor.service;

import com.xcstring.editor.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptServiceTest {

    @TempDir
    Path tempDir;

    private PromptService newServiceWithDir(Path dir) {
        AppProperties props = new AppProperties();
        props.getAi().setPromptsDir(dir.toString());
        PromptService service = new PromptService(props);
        service.initialise();
        return service;
    }

    @Test
    @DisplayName("extracts bundled templates to the config dir on first init")
    void extractsTemplatesOnFirstInit() throws IOException {
        Path promptsDir = tempDir.resolve("prompts");

        newServiceWithDir(promptsDir);

        Path translationFile = promptsDir.resolve(PromptService.TRANSLATION_TEMPLATE);
        Path proofreadingFile = promptsDir.resolve(PromptService.PROOFREADING_TEMPLATE);

        assertTrue(Files.exists(translationFile), "translation.txt should be extracted");
        assertTrue(Files.exists(proofreadingFile), "proofreading.txt should be extracted");

        String extracted = Files.readString(translationFile, StandardCharsets.UTF_8);
        assertTrue(extracted.contains("${sourceLanguage}"), "extracted template should keep placeholders");
        assertTrue(extracted.contains("${targetLanguage}"));
        assertTrue(extracted.contains("${itemsJson}"));
    }

    @Test
    @DisplayName("preserves user customisation: existing files are not overwritten")
    void doesNotOverwriteExistingFiles() throws IOException {
        Path promptsDir = tempDir.resolve("prompts");
        Files.createDirectories(promptsDir);

        Path translationFile = promptsDir.resolve(PromptService.TRANSLATION_TEMPLATE);
        String customContent = "TRANSLATE ${sourceLanguage} -> ${targetLanguage} DATA: ${itemsJson} (custom tone)";
        Files.writeString(translationFile, customContent, StandardCharsets.UTF_8);

        PromptService service = newServiceWithDir(promptsDir);

        assertEquals(customContent, service.getRawTemplate(PromptService.TRANSLATION_TEMPLATE),
            "user customised template must be loaded verbatim, never overwritten");
    }

    @Test
    @DisplayName("renderTranslationPrompt substitutes all variables")
    void rendersTranslationPrompt() {
        Path promptsDir = tempDir.resolve("prompts");
        PromptService service = newServiceWithDir(promptsDir);

        String rendered = service.renderTranslationPrompt("en", "de", "[{\"key\":\"greet\",\"text\":\"hello\"}]");

        assertFalse(rendered.contains("${sourceLanguage}"), "${sourceLanguage} must be replaced");
        assertFalse(rendered.contains("${targetLanguage}"), "${targetLanguage} must be replaced");
        assertFalse(rendered.contains("${itemsJson}"), "${itemsJson} must be replaced");
        assertTrue(rendered.contains("en"), "rendered prompt must contain source language");
        assertTrue(rendered.contains("de"), "rendered prompt must contain target language");
        assertTrue(rendered.contains("[{\"key\":\"greet\",\"text\":\"hello\"}]"),
            "rendered prompt must contain items json");
    }

    @Test
    @DisplayName("renderProofreadingPrompt substitutes all variables")
    void rendersProofreadingPrompt() {
        Path promptsDir = tempDir.resolve("prompts");
        PromptService service = newServiceWithDir(promptsDir);

        String rendered = service.renderProofreadingPrompt("fr", "[{\"key\":\"greet\",\"text\":\"bonjour\"}]");

        assertFalse(rendered.contains("${language}"), "${language} must be replaced");
        assertFalse(rendered.contains("${itemsJson}"), "${itemsJson} must be replaced");
        assertTrue(rendered.contains("fr"));
        assertTrue(rendered.contains("[{\"key\":\"greet\",\"text\":\"bonjour\"}]"));
    }

    @Test
    @DisplayName("null variable values are rendered as empty string")
    void nullVariableBecomesEmpty() {
        Path promptsDir = tempDir.resolve("prompts");
        PromptService service = newServiceWithDir(promptsDir);

        String rendered = service.renderTranslationPrompt(null, "de", "[]");

        assertFalse(rendered.contains("${sourceLanguage}"),
            "null sourceLanguage should be replaced with empty string, not left as placeholder");
    }

    @Test
    @DisplayName("getRawTemplate throws for unknown template name")
    void throwsForUnknownTemplate() {
        Path promptsDir = tempDir.resolve("prompts");
        PromptService service = newServiceWithDir(promptsDir);

        assertThrows(IllegalStateException.class, () -> service.getRawTemplate("nonexistent.txt"));
    }

    @Test
    @DisplayName("loads user-supplied template from the config dir")
    void loadsUserSuppliedTemplateFromConfigDir() throws IOException {
        Path promptsDir = tempDir.resolve("prompts");
        Files.createDirectories(promptsDir);

        Path translationFile = promptsDir.resolve(PromptService.TRANSLATION_TEMPLATE);
        Files.writeString(translationFile, "CUSTOM ${sourceLanguage} ${targetLanguage} ${itemsJson}",
            StandardCharsets.UTF_8);

        PromptService service = newServiceWithDir(promptsDir);
        String rendered = service.renderTranslationPrompt("en", "ja", "[1,2,3]");

        assertTrue(rendered.startsWith("CUSTOM "), "should load the user file from the config dir");
        assertFalse(rendered.contains("${"));
    }
}
