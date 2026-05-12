package com.xcstring.editor.oauth2;

public record OAuth2UserInfo(
    String provider,
    String providerId,
    String email,
    String name,
    String avatar
) {}
