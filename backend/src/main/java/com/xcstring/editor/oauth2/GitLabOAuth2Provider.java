package com.xcstring.editor.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GitLabOAuth2Provider extends AbstractOAuth2Provider {

    public GitLabOAuth2Provider(Map<String, Object> config, String providerName, String baseAppUrl) {
        super(config, providerName, baseAppUrl);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String clientId = getRequiredConfig("client_id");
        String redirectUri = getRedirectUri();
        String instanceUrl = getConfig("instance_url", "https://gitlab.com");
        
        StringBuilder url = new StringBuilder(instanceUrl);
        url.append("/oauth/authorize?");
        url.append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        url.append("&scope=").append(URLEncoder.encode("read_user", StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        
        return url.toString();
    }

    @Override
    public String getAccessToken(String code) {
        String clientId = getRequiredConfig("client_id");
        String clientSecret = getRequiredConfig("client_secret");
        String redirectUri = getRedirectUri();
        String instanceUrl = getConfig("instance_url", "https://gitlab.com");
        
        Map<String, String> formData = new HashMap<>();
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        formData.put("code", code);
        formData.put("grant_type", "authorization_code");
        formData.put("redirect_uri", redirectUri);
        
        try {
            String response = makePostFormRequest(instanceUrl + "/oauth/token", formData);
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
        String instanceUrl = getConfig("instance_url", "https://gitlab.com");
        
        try {
            String response = makeGetRequest(instanceUrl + "/api/v4/user", accessToken);
            Map<String, Object> userData = parseJson(response);
            
            return new OAuth2UserInfo(
                "gitlab",
                userData.get("id").toString(),
                userData.containsKey("email") ? userData.get("email").toString() : null,
                userData.containsKey("name") ? userData.get("name").toString() : null,
                userData.containsKey("avatar_url") ? userData.get("avatar_url").toString() : null
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "GitLab";
    }
}
