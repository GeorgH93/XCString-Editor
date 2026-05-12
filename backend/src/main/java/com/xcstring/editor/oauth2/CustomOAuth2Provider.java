package com.xcstring.editor.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CustomOAuth2Provider extends AbstractOAuth2Provider {

    public CustomOAuth2Provider(Map<String, Object> config, String providerName, String baseAppUrl) {
        super(config, providerName, baseAppUrl);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String clientId = getRequiredConfig("client_id");
        String authorizeUrl = getRequiredConfig("authorize_url");
        String scope = getRequiredConfig("scope");
        String redirectUri = getRedirectUri();
        
        StringBuilder url = new StringBuilder(authorizeUrl);
        url.append("?client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        url.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> additionalParams = (Map<String, Object>) config.get("additional_params");
        if (additionalParams != null) {
            for (Map.Entry<String, Object> param : additionalParams.entrySet()) {
                url.append("&").append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8));
                url.append("=").append(URLEncoder.encode(param.getValue().toString(), StandardCharsets.UTF_8));
            }
        }
        
        return url.toString();
    }

    @Override
    public String getAccessToken(String code) {
        String clientId = getRequiredConfig("client_id");
        String clientSecret = getRequiredConfig("client_secret");
        String tokenUrl = getRequiredConfig("token_url");
        String redirectUri = getRedirectUri();
        
        Map<String, String> formData = new HashMap<>();
        formData.put("client_id", clientId);
        formData.put("client_secret", clientSecret);
        formData.put("code", code);
        formData.put("grant_type", "authorization_code");
        formData.put("redirect_uri", redirectUri);
        
        try {
            String response = makePostFormRequest(tokenUrl, formData);
            Map<String, Object> tokenData = parseJson(response);
            
            if (!tokenData.containsKey("access_token")) {
                throw new RuntimeException("Access token not found in response from custom provider '" + providerName + "'");
            }
            
            return tokenData.get("access_token").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get access token: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2UserInfo getUserInfo(String accessToken) {
        String userInfoUrl = getRequiredConfig("user_info_url");
        String userIdField = getRequiredConfig("user_id_field");
        String userEmailField = getRequiredConfig("user_email_field");
        String userNameField = getRequiredConfig("user_name_field");
        String userAvatarField = getConfig("user_avatar_field", null);
        
        try {
            String response = makeGetRequest(userInfoUrl, accessToken);
            Map<String, Object> userData = parseJson(response);
            
            if (!userData.containsKey(userIdField)) {
                throw new RuntimeException("Field '" + userIdField + "' not found in user info response for provider '" + providerName + "'");
            }
            if (!userData.containsKey(userEmailField)) {
                throw new RuntimeException("Field '" + userEmailField + "' not found in user info response for provider '" + providerName + "'");
            }
            if (!userData.containsKey(userNameField)) {
                throw new RuntimeException("Field '" + userNameField + "' not found in user info response for provider '" + providerName + "'");
            }
            
            String avatar = null;
            if (userAvatarField != null && userData.containsKey(userAvatarField)) {
                avatar = userData.get(userAvatarField) != null ? userData.get(userAvatarField).toString() : null;
            }
            
            return new OAuth2UserInfo(
                providerName,
                userData.get(userIdField).toString(),
                userData.get(userEmailField).toString(),
                userData.get(userNameField).toString(),
                avatar
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        String displayName = getConfig("display_name", null);
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        return capitalize(providerName);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
