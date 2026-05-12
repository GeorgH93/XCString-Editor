package com.xcstring.editor.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MicrosoftOAuth2Provider extends AbstractOAuth2Provider {

    public MicrosoftOAuth2Provider(Map<String, Object> config, String providerName, String baseAppUrl) {
        super(config, providerName, baseAppUrl);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String clientId = getRequiredConfig("client_id");
        String redirectUri = getRedirectUri();
        String tenant = getConfig("tenant", "common");
        
        StringBuilder url = new StringBuilder("https://login.microsoftonline.com/");
        url.append(tenant).append("/oauth2/v2.0/authorize?");
        url.append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        url.append("&scope=").append(URLEncoder.encode("openid email profile User.Read", StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        
        return url.toString();
    }

    @Override
    public String getAccessToken(String code) {
        String clientId = getRequiredConfig("client_id");
        String clientSecret = getRequiredConfig("client_secret");
        String redirectUri = getRedirectUri();
        String tenant = getConfig("tenant", "common");
        
        Map<String, String> formData = new HashMap<>();
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        formData.put("code", code);
        formData.put("grant_type", "authorization_code");
        formData.put("redirect_uri", redirectUri);
        
        try {
            String response = makePostFormRequest(
                "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token", 
                formData
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
            String response = makeGetRequest("https://graph.microsoft.com/v1.0/me", accessToken);
            Map<String, Object> userData = parseJson(response);
            
            String email = null;
            if (userData.containsKey("mail") && userData.get("mail") != null) {
                email = userData.get("mail").toString();
            } else if (userData.containsKey("userPrincipalName")) {
                email = userData.get("userPrincipalName").toString();
            }
            
            return new OAuth2UserInfo(
                "microsoft",
                userData.get("id").toString(),
                email,
                userData.containsKey("displayName") ? userData.get("displayName").toString() : null,
                null
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "Microsoft";
    }
}
