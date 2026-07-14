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
    private final PromptService promptService;
    private final Gson gson = new GsonBuilder().create();

    public AIService(AppProperties appProperties, PromptService promptService) {
        this.appProperties = appProperties;
        this.promptService = promptService;
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
                ? Collections.singletonList("translate")
                : Arrays.asList("translate", "proofread"));
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

        return promptService.renderTranslationPrompt(sourceLanguage, targetLanguage, itemsJson);
    }

    private String buildBatchProofreadingPrompt(List<Map<String, Object>> items, String language) {
        String itemsJson;
        try {
            itemsJson = gson.toJson(items);
        } catch (Exception e) {
            itemsJson = items.toString();
        }

        return promptService.renderProofreadingPrompt(language, itemsJson);
    }
}
