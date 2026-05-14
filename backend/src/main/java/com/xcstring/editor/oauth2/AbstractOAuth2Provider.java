package com.xcstring.editor.oauth2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractOAuth2Provider implements OAuth2Provider {

    protected final OkHttpClient httpClient;
    protected final String baseAppUrl;
    protected final Map<String, Object> config;
    protected final String providerName;
    protected final Gson gson = new Gson();

    protected AbstractOAuth2Provider(Map<String, Object> config, String providerName, String baseAppUrl) {
        this.config = config;
        this.providerName = providerName;
        this.baseAppUrl = baseAppUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    protected String getRedirectUri() {
        Object redirectUri = config.get("redirect_uri");
        if (redirectUri != null && !redirectUri.toString().isEmpty()) {
            return redirectUri.toString();
        }
        return baseAppUrl + "/api/auth/oauth/" + providerName + "/callback";
    }

    protected String getRequiredConfig(String key) {
        Object value = config.get(key);
        if (value == null || value.toString().isEmpty()) {
            throw new RuntimeException("Missing required configuration field for provider '" + providerName + "': " + key);
        }
        return value.toString();
    }

    protected String getConfig(String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    protected String makeGetRequest(String url, String accessToken) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "XCString-Editor/1.0")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with status " + response.code());
            }
            return response.body().string();
        }
    }

    protected String makePostFormRequest(String url, Map<String, String> formData) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }
        RequestBody formBody = formBuilder.build();

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "XCString-Editor/1.0")
            .post(formBody)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with status " + response.code());
            }
            return response.body().string();
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseJson(String json) throws IOException {
        return gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
    }
}
