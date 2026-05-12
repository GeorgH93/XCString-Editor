package com.xcstring.editor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.config.AppProperties.ProviderAiConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AIService {

    private final AppProperties appProperties;
    private final Gson gson = new GsonBuilder().create();

    public AIService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public boolean isEnabled() {
        return appProperties.getAi().isEnabled();
    }

    private boolean isDeepL(String provider) {
        return "deepl".equals(provider);
    }

    public Map<String, Object> getAvailableProviders() {
        Map<String, Object> providers = new LinkedHashMap<>();
        
        if (!isEnabled()) {
            return providers;
        }

        AppProperties.AiProps aiProps = appProperties.getAi();
        
        addProviderIfAvailable(providers, "openai", aiProps.getOpenai());
        addProviderIfAvailable(providers, "anthropic", aiProps.getAnthropic());
        addProviderIfAvailable(providers, "zai", aiProps.getZai());
        addProviderIfAvailable(providers, "deepl", aiProps.getDeepl());
        addProviderIfAvailable(providers, "openai_compatible", aiProps.getOpenaiCompatible());

        return providers;
    }

    private void addProviderIfAvailable(Map<String, Object> providers, String name, ProviderAiConfig config) {
        if (config.isEnabled() && config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            Map<String, Object> providerInfo = new LinkedHashMap<>();
            providerInfo.put("name", name);
            providerInfo.put("models", config.getModels());
            providerInfo.put("capabilities", isDeepL(name)
                ? Arrays.asList("translate", "batch_translate")
                : Arrays.asList("translate", "batch_translate", "proofread", "batch_proofread"));
            providers.put(name, providerInfo);
        }
    }

    private static class ResolvedProvider {
        final String provider;
        final String model;
        final ProviderAiConfig config;

        ResolvedProvider(String provider, String model, ProviderAiConfig config) {
            this.provider = provider;
            this.model = model;
            this.config = config;
        }
    }

    private ResolvedProvider resolveProvider(String provider, String model) {
        AppProperties.AiProps aiProps = appProperties.getAi();
        
        if (provider == null) {
            provider = aiProps.getDefaultProvider();
        }
        if (model == null) {
            model = aiProps.getDefaultModel();
        }

        ProviderAiConfig config = getProviderConfig(provider);
        
        if (config == null || !config.isEnabled()) {
            throw new RuntimeException("Provider " + provider + " is not available");
        }

        return new ResolvedProvider(provider, model, config);
    }

    private ProviderAiConfig getProviderConfig(String provider) {
        AppProperties.AiProps aiProps = appProperties.getAi();
        
        switch (provider) {
            case "openai":
                return aiProps.getOpenai();
            case "anthropic":
                return aiProps.getAnthropic();
            case "zai":
                return aiProps.getZai();
            case "deepl":
                return aiProps.getDeepl();
            case "openai_compatible":
                return aiProps.getOpenaiCompatible();
            default:
                return null;
        }
    }

    public String translate(String text, String sourceLanguage, String targetLanguage,
                           List<Map<String, Object>> context, String stringKey,
                           String provider, String model) {
        if (!isEnabled()) {
            throw new RuntimeException("AI features are not enabled");
        }

        ResolvedProvider resolved = resolveProvider(provider, model);

        if (isDeepL(resolved.provider)) {
            return deepLTranslate(Collections.singletonList(text), sourceLanguage, 
                targetLanguage, resolved.model, resolved.config).get(0);
        }

        String prompt = buildTranslationPrompt(text, sourceLanguage, targetLanguage, context);
        return (String) makeAIRequest(resolved.provider, resolved.model, prompt, resolved.config, false);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> proofread(String text, String language,
                                         List<Map<String, Object>> context, String stringKey,
                                         String provider, String model) {
        if (!isEnabled()) {
            throw new RuntimeException("AI features are not enabled");
        }

        ResolvedProvider resolved = resolveProvider(provider, model);

        if (isDeepL(resolved.provider)) {
            throw new RuntimeException("DeepL does not support proofreading. Please select a different provider.");
        }

        String prompt = buildProofreadingPrompt(text, language, context);
        Object result = makeAIRequest(resolved.provider, resolved.model, prompt, resolved.config, true);
        
        return (Map<String, Object>) result;
    }

    private String buildTranslationPrompt(String text, String sourceLanguage, String targetLanguage,
                                          List<Map<String, Object>> context) {
        StringBuilder contextStr = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            contextStr.append("\n\nContext (related strings):\n");
            for (Map<String, Object> item : context) {
                contextStr.append("- Key: ").append(item.get("key"))
                    .append(", ").append(sourceLanguage).append(": \"")
                    .append(item.get("source")).append("\", ")
                    .append(targetLanguage).append(": \"")
                    .append(item.get("target")).append("\"\n");
            }
        }

        return "Translate the following text from " + sourceLanguage + " to " + targetLanguage + ". This is for a mobile/desktop application localization.\n" +
            "\n" +
            "Source text: \"" + text + "\"\n" +
            "Source language: " + sourceLanguage + "\n" +
            "Target language: " + targetLanguage + "\n" +
            contextStr.toString() + "\n" +
            "Instructions:\n" +
            "- Provide only the translated text, no explanations\n" +
            "- Consider the context of mobile/desktop application UI\n" +
            "- Use an informal, friendly tone appropriate for modern mobile/desktop applications.\n" +
            "- If the target language distinguishes between formal and informal second-person address (T/V distinction), always use the informal form.\n" +
            "- Never mix formal and informal address within the same translation.\n" +
            "- Use the corresponding informal verb conjugations and possessive forms.\n" +
            "- Be consistent throughout all translations.\n" +
            "- Keep placeholders and formatting intact if any\n" +
            "- Consider the context of mobile/desktop application UI\n" +
            "- Do not translate product names, trademarks, or proper nouns unless they have an established localized form in " + targetLanguage + "\n" +
            "- Use the 'key' field to understand the context and purpose of each string\n" +
            "- Do not omit or add entries\n" +
            "- Ensure all output is valid, properly escaped JSON\n" +
            "- Do not modify the 'key' values\n" +
            "- If it's a technical term or brand name, consider if it should remain untranslated\n" +
            "\n" +
            "Translation:";
    }

    private String buildProofreadingPrompt(String text, String language, List<Map<String, Object>> context) {
        StringBuilder contextStr = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            contextStr.append("\n\nContext (related strings in the same language):\n");
            for (Map<String, Object> item : context) {
                contextStr.append("- Key: ").append(item.get("key"))
                    .append(", Text: \"").append(item.get("text")).append("\"\n");
            }
        }

        return "Review the following localized text for a mobile/desktop application. Evaluate the quality and provide feedback.\n" +
            "\n" +
            "Text to review: \"" + text + "\"\n" +
            "Language: " + language + "\n" +
            contextStr.toString() + "\n" +
            "Please respond with a JSON object containing:\n" +
            "{\n" +
            "  \"status\": \"good\" | \"wording\" | \"issue\",\n" +
            "  \"feedback\": \"explanation of any issues or suggestions\"\n" +
            "}\n" +
            "\n" +
            "Status meanings:\n" +
            "- \"good\": The text is well-written and appropriate\n" +
            "- \"wording\": The text is understandable but could be improved with better wording\n" +
            "- \"issue\": There are serious problems (grammar, unclear meaning, inappropriate tone, etc.)\n" +
            "\n" +
            "Consider:\n" +
            "- Grammar and spelling\n" +
            "- Clarity and naturalness\n" +
            "- Appropriateness for UI context\n" +
            "- Consistency with typical app terminology\n" +
            "- Cultural appropriateness\n" +
            "\n" +
            "Response:";
    }

    private Object makeAIRequest(String provider, String model, String prompt, 
                                  ProviderAiConfig config, boolean expectJson) {
        switch (provider) {
            case "openai":
            case "openai_compatible":
                return makeOpenAIRequest(model, prompt, config, expectJson);
            case "anthropic":
                return makeAnthropicRequest(model, prompt, config, expectJson);
            case "zai":
                return makeZAIRequest(model, prompt, config, expectJson);
            default:
                throw new RuntimeException("Unsupported provider: " + provider);
        }
    }

    private OkHttpClient buildClient(int timeoutSeconds) {
        return new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();
    }

    @SuppressWarnings("unchecked")
    private Object makeOpenAIRequest(String model, String prompt, ProviderAiConfig config, boolean expectJson) {
        int timeout = expectJson ? 120 : 30;
        OkHttpClient client = buildClient(timeout);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("model", model);
        data.put("input", prompt);
        data.put("max_tokens", expectJson ? 8000 : 500);
        data.put("temperature", 0.3);

        if (expectJson) {
            Map<String, String> responseFormat = new LinkedHashMap<>();
            responseFormat.put("type", "json_object");
            data.put("response_format", responseFormat);
        }

        String url = config.getBaseUrl() + "/responses";

        try {
            String jsonBody = gson.toJson(data);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("AI request failed: HTTP " + response.code());
                }

                String responseBody = response.body().string();
                Map<String, Object> result = gson.fromJson(responseBody, new TypeToken<Map<String, Object>>(){}.getType());
                
                if (result == null || result.get("output") == null) {
                    throw new RuntimeException("Invalid AI response format");
                }

                List<Map<String, Object>> output = (List<Map<String, Object>>) result.get("output");
                if (output.isEmpty() || output.get(0).get("content") == null) {
                    throw new RuntimeException("Invalid AI response format");
                }

                List<Map<String, Object>> content = (List<Map<String, Object>>) output.get(0).get("content");
                if (content.isEmpty() || content.get(0).get("text") == null) {
                    throw new RuntimeException("Invalid AI response format");
                }

                String contentText = (String) content.get(0).get("text");

                if (expectJson) {
                    return gson.fromJson(contentText, new TypeToken<Map<String, Object>>(){}.getType());
                }

                return contentText;
            }
        } catch (IOException e) {
            throw new RuntimeException("AI request failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object makeAnthropicRequest(String model, String prompt, ProviderAiConfig config, boolean expectJson) {
        int timeout = expectJson ? 120 : 30;
        OkHttpClient client = buildClient(timeout);

        String actualPrompt = prompt;
        if (expectJson) {
            actualPrompt = prompt + "\n\nIMPORTANT: Respond only with valid JSON, no other text.";
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", actualPrompt);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("model", model);
        data.put("max_tokens", expectJson ? 40000 : 500);
        data.put("temperature", 0.3);
        data.put("messages", Collections.singletonList(message));

        String url = config.getBaseUrl() + "/v1/messages";

        try {
            String jsonBody = gson.toJson(data);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", config.getApiKey())
                .addHeader("anthropic-version", "2023-06-01")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("AI request failed: HTTP " + response.code());
                }

                String responseBody = response.body().string();
                Map<String, Object> result = gson.fromJson(responseBody, new TypeToken<Map<String, Object>>(){}.getType());
                
                if (result == null || result.get("content") == null) {
                    throw new RuntimeException("Invalid AI response format");
                }

                List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                if (content.isEmpty() || content.get(0).get("text") == null) {
                    throw new RuntimeException("Invalid AI response format");
                }

                String contentText = ((String) content.get(0).get("text")).trim();

                if (expectJson) {
                    return gson.fromJson(contentText, new TypeToken<Map<String, Object>>(){}.getType());
                }

                return contentText;
            }
        } catch (IOException e) {
            throw new RuntimeException("AI request failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object makeZAIRequest(String model, String prompt, ProviderAiConfig config, boolean expectJson) {
        int timeout = expectJson ? 120 : 30;
        OkHttpClient client = buildClient(timeout);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("model", model);
        data.put("messages", Collections.singletonList(message));
        data.put("max_tokens", expectJson ? 40000 : 500);
        data.put("temperature", 0.3);

        if (expectJson) {
            Map<String, String> responseFormat = new LinkedHashMap<>();
            responseFormat.put("type", "json_object");
            data.put("response_format", responseFormat);
        }

        String url = config.getBaseUrl() + "/chat/completions";

        try {
            String jsonBody = gson.toJson(data);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Z.AI request failed: HTTP " + response.code());
                }

                String responseBody = response.body().string();
                Map<String, Object> result = gson.fromJson(responseBody, new TypeToken<Map<String, Object>>(){}.getType());
                
                if (result == null || result.get("choices") == null) {
                    throw new RuntimeException("Invalid Z.AI response format");
                }

                List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                if (choices.isEmpty() || choices.get(0).get("message") == null) {
                    throw new RuntimeException("Invalid Z.AI response format");
                }

                Map<String, Object> messageResult = (Map<String, Object>) choices.get(0).get("message");
                if (messageResult.get("content") == null) {
                    throw new RuntimeException("Invalid Z.AI response format");
                }

                String contentText = ((String) messageResult.get("content")).trim();

                if (expectJson) {
                    return gson.fromJson(contentText, new TypeToken<Map<String, Object>>(){}.getType());
                }

                return contentText;
            }
        } catch (IOException e) {
            throw new RuntimeException("Z.AI request failed: " + e.getMessage(), e);
        }
    }

    private String getDeepLBaseUrl(ProviderAiConfig config) {
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            return config.getBaseUrl();
        }
        
        String apiKey = config.getApiKey();
        if (apiKey != null && apiKey.endsWith(":fx")) {
            return "https://api-free.deepl.com";
        }
        return "https://api.deepl.com";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> callDeepLApi(List<String> texts, String sourceLanguage, 
                                                    String targetLanguage, String model, 
                                                    ProviderAiConfig config) {
        int timeout = texts.size() > 1 ? 120 : 30;
        OkHttpClient client = buildClient(timeout);
        
        String baseUrl = getDeepLBaseUrl(config);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", texts);
        data.put("target_lang", targetLanguage.toUpperCase());
        data.put("model_type", model);
        data.put("formality", "prefer_less");
        data.put("split_sentences", "0");

        if (sourceLanguage != null && !sourceLanguage.isEmpty()) {
            data.put("source_lang", sourceLanguage.toUpperCase());
        }

        String url = baseUrl + "/v2/translate";

        try {
            String jsonBody = gson.toJson(data);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "DeepL-Auth-Key " + config.getApiKey())
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("DeepL API request failed: HTTP " + response.code());
                }

                String responseBody = response.body().string();
                Map<String, Object> result = gson.fromJson(responseBody, new TypeToken<Map<String, Object>>(){}.getType());
                
                if (result == null || result.get("translations") == null) {
                    throw new RuntimeException("Invalid DeepL API response format");
                }

                return (List<Map<String, Object>>) result.get("translations");
            }
        } catch (IOException e) {
            throw new RuntimeException("DeepL API request failed: " + e.getMessage(), e);
        }
    }

    private List<String> deepLTranslate(List<String> texts, String sourceLanguage, 
                                         String targetLanguage, String model, 
                                         ProviderAiConfig config) {
        List<Map<String, Object>> translations = callDeepLApi(texts, sourceLanguage, 
            targetLanguage, model, config);
        
        List<String> result = new ArrayList<>();
        for (Map<String, Object> t : translations) {
            result.add(((String) t.get("text")).trim());
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deepLBatchTranslate(List<Map<String, Object>> items, 
                                                          String sourceLanguage, 
                                                          String targetLanguage, 
                                                          String model, 
                                                          ProviderAiConfig config) {
        List<String> texts = new ArrayList<>();
        for (Map<String, Object> item : items) {
            texts.add((String) item.get("text"));
        }

        List<Map<String, Object>> translations = callDeepLApi(texts, sourceLanguage, 
            targetLanguage, model, config);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("key", items.get(i).get("key"));
            
            String translation = "";
            if (i < translations.size() && translations.get(i).get("text") != null) {
                translation = ((String) translations.get(i).get("text")).trim();
            }
            resultMap.put("translation", translation);
            
            result.add(resultMap);
        }

        return result;
    }

    public List<Map<String, Object>> buildContext(String currentKey, Map<String, Object> allStrings, 
                                                   String language, int maxItems) {
        List<Map<String, Object>> context = new ArrayList<>();
        int count = 0;

        if (allStrings == null) {
            return context;
        }

        for (Map.Entry<String, Object> entry : allStrings.entrySet()) {
            if (count >= maxItems || entry.getKey().equals(currentKey)) {
                continue;
            }

            Map<String, Object> stringData = (Map<String, Object>) entry.getValue();
            if (stringData != null && stringData.get("localizations") != null) {
                Map<String, Object> localizations = (Map<String, Object>) stringData.get("localizations");
                if (localizations.get(language) != null) {
                    Map<String, Object> langData = (Map<String, Object>) localizations.get(language);
                    if (langData.get("stringUnit") != null) {
                        Map<String, Object> stringUnit = (Map<String, Object>) langData.get("stringUnit");
                        if (stringUnit.get("value") != null) {
                            Map<String, Object> contextItem = new LinkedHashMap<>();
                            contextItem.put("key", entry.getKey());
                            contextItem.put("text", stringUnit.get("value"));
                            context.add(contextItem);
                            count++;
                        }
                    }
                }
            }
        }

        return context;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buildTranslationContext(String currentKey, 
                                                              Map<String, Object> allStrings,
                                                              String sourceLanguage, 
                                                              String targetLanguage, 
                                                              int maxItems) {
        List<Map<String, Object>> context = new ArrayList<>();
        int count = 0;

        if (allStrings == null) {
            return context;
        }

        for (Map.Entry<String, Object> entry : allStrings.entrySet()) {
            if (count >= maxItems || entry.getKey().equals(currentKey)) {
                continue;
            }

            Map<String, Object> stringData = (Map<String, Object>) entry.getValue();
            if (stringData != null && stringData.get("localizations") != null) {
                Map<String, Object> localizations = (Map<String, Object>) stringData.get("localizations");
                
                String sourceText = null;
                String targetText = null;
                
                if (localizations.get(sourceLanguage) != null) {
                    Map<String, Object> sourceLangData = (Map<String, Object>) localizations.get(sourceLanguage);
                    if (sourceLangData.get("stringUnit") != null) {
                        Map<String, Object> sourceStringUnit = (Map<String, Object>) sourceLangData.get("stringUnit");
                        sourceText = (String) sourceStringUnit.get("value");
                    }
                }
                
                if (localizations.get(targetLanguage) != null) {
                    Map<String, Object> targetLangData = (Map<String, Object>) localizations.get(targetLanguage);
                    if (targetLangData.get("stringUnit") != null) {
                        Map<String, Object> targetStringUnit = (Map<String, Object>) targetLangData.get("stringUnit");
                        targetText = (String) targetStringUnit.get("value");
                    }
                }

                if (sourceText != null && !sourceText.isEmpty() && 
                    targetText != null && !targetText.isEmpty()) {
                    Map<String, Object> contextItem = new LinkedHashMap<>();
                    contextItem.put("key", entry.getKey());
                    contextItem.put("source", sourceText);
                    contextItem.put("target", targetText);
                    context.add(contextItem);
                    count++;
                }
            }
        }

        return context;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> batchTranslate(List<Map<String, Object>> items, 
                                                    String sourceLanguage, 
                                                    String targetLanguage, 
                                                    String provider, 
                                                    String model) {
        if (!isEnabled()) {
            throw new RuntimeException("AI features are not enabled");
        }

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        ResolvedProvider resolved = resolveProvider(provider, model);

        if (isDeepL(resolved.provider)) {
            return deepLBatchTranslate(items, sourceLanguage, targetLanguage, 
                resolved.model, resolved.config);
        }

        String prompt = buildBatchTranslationPrompt(items, sourceLanguage, targetLanguage);
        Object result = makeAIRequest(resolved.provider, resolved.model, prompt, 
            resolved.config, true);

        Map<String, Object> resultMap = (Map<String, Object>) result;
        if (resultMap.get("translations") == null) {
            throw new RuntimeException("Invalid batch translation response format");
        }

        return (List<Map<String, Object>>) resultMap.get("translations");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> batchProofread(List<Map<String, Object>> items, 
                                                    String language, 
                                                    String provider, 
                                                    String model) {
        if (!isEnabled()) {
            throw new RuntimeException("AI features are not enabled");
        }

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        ResolvedProvider resolved = resolveProvider(provider, model);

        if (isDeepL(resolved.provider)) {
            throw new RuntimeException("DeepL does not support proofreading. Please select a different provider.");
        }

        String prompt = buildBatchProofreadingPrompt(items, language);
        Object result = makeAIRequest(resolved.provider, resolved.model, prompt, 
            resolved.config, true);

        Map<String, Object> resultMap = (Map<String, Object>) result;
        if (resultMap.get("reviews") == null) {
            throw new RuntimeException("Invalid batch proofreading response format");
        }

        return (List<Map<String, Object>>) resultMap.get("reviews");
    }

    private String buildBatchTranslationPrompt(List<Map<String, Object>> items, 
                                                String sourceLanguage, 
                                                String targetLanguage) {
        String itemsJson;
        try {
            itemsJson = gson.toJson(items);
        } catch (Exception e) {
            itemsJson = items.toString();
        }

        return "Translate the following strings from " + sourceLanguage + " to " + targetLanguage + ". This is for a mobile/desktop application localization.\n" +
            "\n" +
            "Input data structure:\n" +
            itemsJson + "\n" +
            "\n" +
            "Instructions:\n" +
            "- Translate each 'text' field from " + sourceLanguage + " to " + targetLanguage + "\n" +
            "- Consider the context of mobile/desktop application UI\n" +
            "- Use an informal, friendly tone appropriate for modern mobile/desktop applications.\n" +
            "- If the target language distinguishes between formal and informal second-person address (T/V distinction), always use the informal form.\n" +
            "- Never mix formal and informal address within the same translation.\n" +
            "- Use the corresponding informal verb conjugations and possessive forms.\n" +
            "- Be consistent throughout all translations.\n" +
            "- Keep placeholders and formatting intact if any\n" +
            "- Consider the context of mobile/desktop application UI\n" +
            "- Do not translate product names, trademarks, or proper nouns unless they have an established localized form in " + targetLanguage + "\n" +
            "- Use the 'key' field to understand the context and purpose of each string\n" +
            "- Do not omit or add entries\n" +
            "- Ensure all output is valid, properly escaped JSON\n" +
            "- Do not modify the 'key' values\n" +
            "\n" +
            "Respond with a JSON object in this exact format:\n" +
            "{\n" +
            "  \"translations\": [\n" +
            "    {\n" +
            "      \"key\": \"original_key_1\",\n" +
            "      \"translation\": \"translated_text_1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"key\": \"original_key_2\", \n" +
            "      \"translation\": \"translated_text_2\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "\n" +
            "Important:\n" +
            "- Respond only with valid JSON\n" +
            "- Do not include explanations or additional fields";
    }

    private String buildBatchProofreadingPrompt(List<Map<String, Object>> items, String language) {
        String itemsJson;
        try {
            itemsJson = gson.toJson(items);
        } catch (Exception e) {
            itemsJson = items.toString();
        }

        return "Review the following localized texts for a mobile/desktop application. Evaluate the quality and provide feedback for each.\n" +
            "\n" +
            "Input data structure:\n" +
            itemsJson + "\n" +
            "\n" +
            "Language: " + language + "\n" +
            "\n" +
            "For each text, consider:\n" +
            "- Grammar and spelling\n" +
            "- Clarity and naturalness  \n" +
            "- Appropriateness for UI context\n" +
            "- Consistency with typical app terminology\n" +
            "- Cultural appropriateness\n" +
            "\n" +
            "Respond with a JSON object in this exact format:\n" +
            "{\n" +
            "  \"reviews\": [\n" +
            "    {\n" +
            "      \"key\": \"original_key_1\",\n" +
            "      \"status\": \"good\",\n" +
            "      \"feedback\": \"explanation or empty string if good\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"key\": \"original_key_2\",\n" +
            "      \"status\": \"wording\", \n" +
            "      \"feedback\": \"explanation of suggestions\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "\n" +
            "Status meanings:\n" +
            "- \"good\": The text is well-written and appropriate\n" +
            "- \"wording\": The text is understandable but could be improved with better wording\n" +
            "- \"issue\": There are serious problems (grammar, unclear meaning, inappropriate tone, etc.)\n" +
            "\n" +
            "Important: Respond only with valid JSON, no other text.";
    }
}
