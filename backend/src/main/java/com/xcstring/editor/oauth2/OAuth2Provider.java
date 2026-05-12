package com.xcstring.editor.oauth2;

public interface OAuth2Provider {
    
    String getAuthorizationUrl(String state);
    
    OAuth2UserInfo getUserInfo(String accessToken);
    
    String getProviderName();
    
    default String getAccessToken(String code) {
        throw new UnsupportedOperationException("getAccessToken not implemented");
    }
}
