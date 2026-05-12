package com.xcstring.editor.oauth2;

import com.xcstring.editor.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class OAuth2ProviderFactory {

    private static final Set<String> BUILT_IN_PROVIDERS = Set.of("google", "github", "microsoft", "gitlab");

    public OAuth2Provider create(String providerName, AppProperties appProperties) {
        String baseAppUrl = appProperties.getApp().getBaseUrl();
        AppProperties.OAuth2Props oauth2Props = appProperties.getOauth2();
        
        return switch (providerName) {
            case "google" -> {
                Map<String, Object> config = providerPropsToMap(oauth2Props.getGoogle());
                yield new GoogleOAuth2Provider(config, providerName, baseAppUrl);
            }
            case "github" -> {
                Map<String, Object> config = providerPropsToMap(oauth2Props.getGithub());
                yield new GitHubOAuth2Provider(config, providerName, baseAppUrl);
            }
            case "microsoft" -> {
                Map<String, Object> config = microsoftPropsToMap(oauth2Props.getMicrosoft());
                yield new MicrosoftOAuth2Provider(config, providerName, baseAppUrl);
            }
            case "gitlab" -> {
                Map<String, Object> config = gitLabPropsToMap(oauth2Props.getGitlab());
                yield new GitLabOAuth2Provider(config, providerName, baseAppUrl);
            }
            default -> {
                if (isCustomProvider(providerName, oauth2Props)) {
                    Map<String, Object> config = customPropsToMap(oauth2Props.getCustomProviders().get(providerName));
                    yield new CustomOAuth2Provider(config, providerName, baseAppUrl);
                }
                throw new RuntimeException("Unsupported OAuth2 provider: " + providerName);
            }
        };
    }

    public Map<String, Map<String, Object>> getAvailableProviders(AppProperties appProperties) {
        Map<String, Map<String, Object>> providers = new LinkedHashMap<>();
        
        AppProperties.OAuth2Props oauth2Props = appProperties.getOauth2();
        if (!oauth2Props.isEnabled()) {
            return providers;
        }

        addBuiltInProvider(providers, "google", oauth2Props.getGoogle(), appProperties);
        addBuiltInProvider(providers, "github", oauth2Props.getGithub(), appProperties);
        addBuiltInProvider(providers, "microsoft", oauth2Props.getMicrosoft(), appProperties);
        addBuiltInProvider(providers, "gitlab", oauth2Props.getGitlab(), appProperties);

        Map<String, AppProperties.CustomProviderProps> customProviders = oauth2Props.getCustomProviders();
        if (customProviders != null) {
            for (Map.Entry<String, AppProperties.CustomProviderProps> entry : customProviders.entrySet()) {
                String name = entry.getKey();
                AppProperties.CustomProviderProps props = entry.getValue();
                
                if (props.isEnabled() && props.getClientId() != null && !props.getClientId().isEmpty()) {
                    OAuth2Provider provider = create(name, appProperties);
                    Map<String, Object> providerInfo = new LinkedHashMap<>();
                    providerInfo.put("name", name);
                    providerInfo.put("display_name", provider.getProviderName());
                    providerInfo.put("icon_svg", props.getIconSvg());
                    providerInfo.put("allow_registration", props.isAllowRegistration());
                    providers.put(name, providerInfo);
                }
            }
        }

        return providers;
    }

    private void addBuiltInProvider(
            Map<String, Map<String, Object>> providers, 
            String name, 
            AppProperties.ProviderProps props,
            AppProperties appProperties) {
        
        if (props.isEnabled() && props.getClientId() != null && !props.getClientId().isEmpty()) {
            OAuth2Provider provider = create(name, appProperties);
            Map<String, Object> providerInfo = new LinkedHashMap<>();
            providerInfo.put("name", name);
            providerInfo.put("display_name", provider.getProviderName());
            providerInfo.put("icon_svg", null);
            providers.put(name, providerInfo);
        }
    }

    private boolean isCustomProvider(String providerName, AppProperties.OAuth2Props oauth2Props) {
        if (BUILT_IN_PROVIDERS.contains(providerName)) {
            return false;
        }
        
        Map<String, AppProperties.CustomProviderProps> customProviders = oauth2Props.getCustomProviders();
        return customProviders != null && customProviders.containsKey(providerName);
    }

    private Map<String, Object> providerPropsToMap(AppProperties.ProviderProps props) {
        Map<String, Object> map = new HashMap<>();
        map.put("client_id", props.getClientId());
        map.put("client_secret", props.getClientSecret());
        return map;
    }

    private Map<String, Object> microsoftPropsToMap(AppProperties.MicrosoftProviderProps props) {
        Map<String, Object> map = providerPropsToMap(props);
        map.put("tenant", props.getTenant());
        return map;
    }

    private Map<String, Object> gitLabPropsToMap(AppProperties.GitLabProviderProps props) {
        Map<String, Object> map = providerPropsToMap(props);
        map.put("instance_url", props.getInstanceUrl());
        return map;
    }

    private Map<String, Object> customPropsToMap(AppProperties.CustomProviderProps props) {
        Map<String, Object> map = new HashMap<>();
        map.put("client_id", props.getClientId());
        map.put("client_secret", props.getClientSecret());
        map.put("display_name", props.getDisplayName());
        map.put("authorize_url", props.getAuthorizeUrl());
        map.put("token_url", props.getTokenUrl());
        map.put("user_info_url", props.getUserInfoUrl());
        map.put("scope", props.getScope());
        map.put("user_id_field", props.getUserIdField());
        map.put("user_name_field", props.getUserNameField());
        map.put("user_email_field", props.getUserEmailField());
        map.put("user_avatar_field", props.getUserAvatarField());
        map.put("icon_svg", props.getIconSvg());
        map.put("allow_registration", props.isAllowRegistration());
        
        if (props.getAdditionalParams() != null) {
            map.put("additional_params", new HashMap<>(props.getAdditionalParams()));
        }
        
        return map;
    }
}
