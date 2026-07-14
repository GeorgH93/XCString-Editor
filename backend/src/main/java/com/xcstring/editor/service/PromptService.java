package com.xcstring.editor.service;

import com.xcstring.editor.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads AI prompt templates from {@code xcstring.ai.prompts-dir}. Bundled templates
 * ({@code classpath:prompts/*.txt}) are extracted on first start and never overwritten,
 * so users can edit the extracted files to customise tone or instructions.
 * Templates use {@code ${var}} placeholders replaced via {@link String#replace}.
 */
@Service
@Slf4j
public class PromptService {

    public static final String TRANSLATION_TEMPLATE = "translation.txt";
    public static final String PROOFREADING_TEMPLATE = "proofreading.txt";

    private static final String CLASSPATH_PREFIX = "prompts/";

    private final AppProperties appProperties;
    private final Map<String, String> templates = new LinkedHashMap<>();

    public PromptService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void initialise() {
        String promptsDir = appProperties.getAi().getPromptsDir();
        log.info("Initialising prompt templates from '{}' (configured dir)", promptsDir);

        extractIfMissing(TRANSLATION_TEMPLATE);
        extractIfMissing(PROOFREADING_TEMPLATE);

        templates.put(TRANSLATION_TEMPLATE, loadTemplate(TRANSLATION_TEMPLATE));
        templates.put(PROOFREADING_TEMPLATE, loadTemplate(PROOFREADING_TEMPLATE));
    }

    public String renderTranslationPrompt(String sourceLanguage, String targetLanguage, String itemsJson) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("sourceLanguage", sourceLanguage);
        vars.put("targetLanguage", targetLanguage);
        vars.put("itemsJson", itemsJson);
        return render(TRANSLATION_TEMPLATE, vars);
    }

    public String renderProofreadingPrompt(String language, String itemsJson) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("language", language);
        vars.put("itemsJson", itemsJson);
        return render(PROOFREADING_TEMPLATE, vars);
    }

    public String getRawTemplate(String name) {
        String template = templates.get(name);
        if (template == null) {
            throw new IllegalStateException("Unknown prompt template: " + name);
        }
        return template;
    }

    private String render(String templateName, Map<String, String> variables) {
        String template = templates.get(templateName);
        if (template == null) {
            throw new IllegalStateException("Prompt template not loaded: " + templateName);
        }

        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace("${" + entry.getKey() + "}", value);
        }
        return rendered;
    }

    private void extractIfMissing(String templateName) {
        Path target = resolveConfigPath(templateName);
        if (Files.exists(target)) {
            log.debug("Prompt template '{}' already exists at {}, keeping user customisation",
                templateName, target);
            return;
        }

        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            log.warn("Cannot create prompt config dir '{}' - will fall back to bundled classpath template: {}",
                target.getParent(), e.getMessage());
            return;
        }

        String bundled = readClasspathTemplate(templateName);
        if (bundled == null) {
            log.warn("Bundled prompt template '{}' not found on classpath - skipping extraction", templateName);
            return;
        }

        try {
            Files.writeString(target, bundled, StandardCharsets.UTF_8);
            log.info("Extracted prompt template to {}", target);
        } catch (IOException e) {
            log.warn("Failed to write prompt template to {} - will fall back to bundled classpath template: {}",
                target, e.getMessage());
        }
    }

    private String loadTemplate(String templateName) {
        Path configPath = resolveConfigPath(templateName);
        if (Files.isReadable(configPath)) {
            try {
                return Files.readString(configPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Failed to read prompt template from {} - falling back to bundled classpath template: {}",
                    configPath, e.getMessage());
            }
        }
        String classpathTemplate = readClasspathTemplate(templateName);
        if (classpathTemplate == null) {
            throw new IllegalStateException(
                "Prompt template '" + templateName + "' missing from both config dir and classpath");
        }
        return classpathTemplate;
    }

    private Path resolveConfigPath(String templateName) {
        return Paths.get(appProperties.getAi().getPromptsDir(), templateName);
    }

    private String readClasspathTemplate(String templateName) {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_PREFIX + templateName);
        if (!resource.exists()) {
            return null;
        }
        try (var input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read bundled prompt template '{}': {}", templateName, e.getMessage());
            return null;
        }
    }

    Map<String, String> getLoadedTemplates() {
        return Collections.unmodifiableMap(templates);
    }
}
