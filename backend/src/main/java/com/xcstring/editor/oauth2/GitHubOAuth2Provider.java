package com.xcstring.editor.oauth2;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubOAuth2Provider extends AbstractOAuth2Provider {

    public GitHubOAuth2Provider(Map<String, Object> config, String providerName, String baseAppUrl) {
        super(config, providerName, baseAppUrl);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String clientId = getRequiredConfig("client_id");
        String redirectUri = getRedirectUri();
        
        StringBuilder url = new StringBuilder("https://github.com/login/oauth/authorize?");
        url.append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        url.append("&scope=").append(URLEncoder.encode("user:email", StandardCharsets.UTF_8));
        url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        
        return url.toString();
    }

    @Override
    public String getAccessToken(String code) {
        String clientId = getRequiredConfig("client_id");
        String clientSecret = getRequiredConfig("client_secret");
        
        Map<String, String> formData = new HashMap<>();
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        formData.put("code", code);
        
        try {
            String response = makePostFormRequestWithAcceptHeader(
                "https://github.com/login/oauth/access_token", 
                formData,
                "application/json"
            );
            Map<String, Object> tokenData = parseJson(response);
            
            if (!tokenData.containsKey("access_token")) {
                throw new RuntimeException("Access token not found in response");
            }
            
            return tokenData.get("access_token").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get access token: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2UserInfo getUserInfo(String accessToken) {
        try {
            String userResponse = makeGetRequest("https://api.github.com/user", accessToken);
            Map<String, Object> userData = parseJson(userResponse);
            
            String email = null;
            if (userData.containsKey("email") && userData.get("email") != null) {
                email = userData.get("email").toString();
            }
            
            if (email == null) {
                String emailsResponse = makeGetRequest("https://api.github.com/user/emails", accessToken);
                List<Map<String, Object>> emailData = gson.fromJson(
                    emailsResponse, 
                    new TypeToken<List<Map<String, Object>>>(){}.getType()
                );
                
                for (Map<String, Object> emailEntry : emailData) {
                    if (Boolean.TRUE.equals(emailEntry.get("primary"))) {
                        email = emailEntry.get("email").toString();
                        break;
                    }
                }
                
                if (email == null && !emailData.isEmpty()) {
                    email = emailData.get(0).get("email").toString();
                }
            }
            
            String name = userData.containsKey("name") && userData.get("name") != null 
                ? userData.get("name").toString() 
                : userData.get("login").toString();
            
            return new OAuth2UserInfo(
                "github",
                userData.get("id").toString(),
                email,
                name,
                userData.containsKey("avatar_url") ? userData.get("avatar_url").toString() : null
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "GitHub";
    }

    private String makePostFormRequestWithAcceptHeader(String url, Map<String, String> formData, String accept) throws IOException {
        okhttp3.FormBody.Builder formBuilder = new okhttp3.FormBody.Builder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }
        okhttp3.RequestBody formBody = formBuilder.build();

        okhttp3.Request request = new okhttp3.Request.Builder()
            .url(url)
            .addHeader("Accept", accept)
            .addHeader("User-Agent", "XCString-Editor/1.0")
            .post(formBody)
            .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with status " + response.code());
            }
            return response.body().string();
        }
    }
}
