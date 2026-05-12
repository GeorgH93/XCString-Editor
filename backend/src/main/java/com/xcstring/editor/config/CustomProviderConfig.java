package com.xcstring.editor.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class CustomProviderConfig {

    private static final String PREFIX = "OAUTH2_CUSTOM_PROVIDER_";

    private static final Map<String, String> FIELD_MAP = Map.ofEntries(
            Map.entry("DISPLAY_NAME", "displayName"),
            Map.entry("CLIENT_ID", "clientId"),
            Map.entry("CLIENT_SECRET", "clientSecret"),
            Map.entry("AUTHORIZE_URL", "authorizeUrl"),
            Map.entry("TOKEN_URL", "tokenUrl"),
            Map.entry("USER_INFO_URL", "userInfoUrl"),
            Map.entry("SCOPE", "scope"),
            Map.entry("USER_ID_FIELD", "userIdField"),
            Map.entry("USER_NAME_FIELD", "userNameField"),
            Map.entry("USER_EMAIL_FIELD", "userEmailField"),
            Map.entry("USER_AVATAR_FIELD", "userAvatarField"),
            Map.entry("ICON_SVG", "iconSvg"),
            Map.entry("ADDITIONAL_PARAMS", "additionalParams"),
            Map.entry("ALLOW_REGISTRATION", "allowRegistration")
    );

    private final AppProperties appProperties;

    Map<String, String> getEnv() {
        return System.getenv();
    }

    @PostConstruct
    void registerCustomProviders() {
        Map<String, String> env = getEnv();
        Set<String> enabledProviders = discoverEnabledProviders(env);

        Map<String, AppProperties.CustomProviderProps> customProviders = appProperties.getOauth2().getCustomProviders();
        if (customProviders == null) {
            customProviders = new HashMap<>();
            appProperties.getOauth2().setCustomProviders(customProviders);
        }

        for (String provider : enabledProviders) {
            String providerPrefix = PREFIX + provider.toUpperCase() + "_";
            AppProperties.CustomProviderProps props = customProviders.computeIfAbsent(
                    provider, k -> new AppProperties.CustomProviderProps());
            props.setEnabled(true);

            for (Map.Entry<String, String> field : FIELD_MAP.entrySet()) {
                String envKey = providerPrefix + field.getKey();
                String value = env.get(envKey);
                if (value != null) {
                    setProperty(props, field.getValue(), value);
                }
            }
        }
    }

    private Set<String> discoverEnabledProviders(Map<String, String> env) {
        Set<String> providers = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(PREFIX) || !key.endsWith("_ENABLED")) {
                continue;
            }
            String providerPart = key.substring(PREFIX.length(), key.length() - "_ENABLED".length());
            if (providerPart.isEmpty()) {
                continue;
            }
            if (Boolean.parseBoolean(entry.getValue())) {
                providers.add(providerPart.toLowerCase());
            }
        }
        return providers;
    }

    private void setProperty(AppProperties.CustomProviderProps props, String fieldName, String value) {
        switch (fieldName) {
            case "displayName" -> props.setDisplayName(value);
            case "clientId" -> props.setClientId(value);
            case "clientSecret" -> props.setClientSecret(value);
            case "authorizeUrl" -> props.setAuthorizeUrl(value);
            case "tokenUrl" -> props.setTokenUrl(value);
            case "userInfoUrl" -> props.setUserInfoUrl(value);
            case "scope" -> props.setScope(value);
            case "userIdField" -> props.setUserIdField(value);
            case "userNameField" -> props.setUserNameField(value);
            case "userEmailField" -> props.setUserEmailField(value);
            case "userAvatarField" -> props.setUserAvatarField(value);
            case "iconSvg" -> props.setIconSvg(value);
            case "allowRegistration" -> props.setAllowRegistration(Boolean.parseBoolean(value));
        }
    }
}
