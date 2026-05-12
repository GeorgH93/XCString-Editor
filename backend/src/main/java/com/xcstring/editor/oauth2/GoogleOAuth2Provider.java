package com.xcstring.editor.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GoogleOAuth2Provider extends AbstractOAuth2Provider {

    public GoogleOAuth2Provider(Map<String, Object> config, String providerName, String baseAppUrl) {
        super(config, providerName, baseAppUrl);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String clientId = getRequiredConfig("client_id");
        String redirectUri = getRedirectUri();
        
        StringBuilder url = new StringBuilder("https://accounts.google.com/o/oauth2/v2/auth?");
        url.append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        url.append("&scope=").append(URLEncoder.encode("openid email profile", StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        url.append("&access_type=offline");
        url.append("&prompt=consent");
        
        return url.toString();
    }

    @Override
    public String getAccessToken(String code) {
        String clientId = getRequiredConfig("client_id");
        String clientSecret = getRequiredConfig("client_secret");
        String redirectUri = getRedirectUri();
        
        Map<String, String> formData = new HashMap<>();
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        formData.put("code", code);
        formData.put("grant_type", "authorization_code");
        formData.put("redirect_uri", redirectUri);
        
        try {
            String response = makePostFormRequest("https://oauth2.googleapis.com/token", formData);
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
            String response = makeGetRequest("https://www.googleapis.com/oauth2/v2/userinfo", accessToken);
            Map<String, Object> userData = parseJson(response);
            
            return new OAuth2UserInfo(
                "google",
                userData.get("id").toString(),
                userData.get("email").toString(),
                userData.get("name").toString(),
                userData.containsKey("picture") ? userData.get("picture").toString() : null
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "Google";
    }
}
