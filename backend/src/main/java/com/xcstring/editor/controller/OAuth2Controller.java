package com.xcstring.editor.controller;

import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.oauth2.OAuth2Provider;
import com.xcstring.editor.oauth2.OAuth2ProviderFactory;
import com.xcstring.editor.oauth2.OAuth2UserInfo;
import com.xcstring.editor.service.AuthService;
import com.xcstring.editor.service.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/backend/index.php")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final AuthService authService;
    private final OAuth2ProviderFactory oauth2ProviderFactory;
    private final AppProperties appProperties;
    private final SessionService sessionService;

    private static final Set<String> BUILT_IN_PROVIDERS = Set.of("google", "github", "microsoft", "gitlab");

    @GetMapping("/auth/oauth/{provider}/redirect")
    public RedirectView redirect(@PathVariable String provider) {
        String baseUrl = appProperties.getApp().getBaseUrl();
        
        try {
            if (!appProperties.getOauth2().isEnabled()) {
                return new RedirectView(baseUrl + "?oauth_error=OAuth2+authentication+is+not+enabled");
            }

            Map<String, Map<String, Object>> availableProviders = oauth2ProviderFactory.getAvailableProviders(appProperties);
            if (!availableProviders.containsKey(provider)) {
                String encodedProvider = URLEncoder.encode(provider, StandardCharsets.UTF_8);
                return new RedirectView(baseUrl + "?oauth_error=Provider+%27" + encodedProvider + "%27+is+not+configured");
            }

            OAuth2Provider oauthProvider = oauth2ProviderFactory.create(provider, appProperties);
            String state = authService.generateOAuth2State(provider);
            String authUrl = oauthProvider.getAuthorizationUrl(state);

            return new RedirectView(authUrl);
        } catch (Exception e) {
            String encodedError = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return new RedirectView(baseUrl + "?oauth_error=" + encodedError);
        }
    }

    @GetMapping("/auth/oauth/{provider}/callback")
    public RedirectView callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) {
        
        String baseUrl = appProperties.getApp().getBaseUrl();
        
        try {
            if (error != null && !error.isEmpty()) {
                String encodedError = URLEncoder.encode(error, StandardCharsets.UTF_8);
                return new RedirectView(baseUrl + "?oauth_error=" + encodedError);
            }

            if (!appProperties.getOauth2().isEnabled()) {
                return new RedirectView(baseUrl + "?oauth_error=OAuth2+authentication+is+not+enabled");
            }

            boolean stateValid = authService.verifyOAuth2State(state, provider);
            if (!stateValid) {
                return new RedirectView(baseUrl + "?oauth_error=Invalid+OAuth2+state");
            }

            OAuth2Provider oauthProvider = oauth2ProviderFactory.create(provider, appProperties);
            String accessToken = oauthProvider.getAccessToken(code);
            OAuth2UserInfo userInfo = oauthProvider.getUserInfo(accessToken);

            Map<String, Object> userInfoMap = new LinkedHashMap<>();
            userInfoMap.put("provider_id", userInfo.providerId());
            userInfoMap.put("email", userInfo.email());
            userInfoMap.put("name", userInfo.name());
            userInfoMap.put("avatar", userInfo.avatar());

            boolean allowRegistration = isRegistrationAllowed(provider);
            authService.handleOAuth2Login(userInfoMap, provider, allowRegistration);

            return new RedirectView(baseUrl + "?oauth_success=1");
        } catch (Exception e) {
            String encodedError = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return new RedirectView(baseUrl + "?oauth_error=" + encodedError);
        }
    }

    private boolean isRegistrationAllowed(String provider) {
        if (BUILT_IN_PROVIDERS.contains(provider)) {
            return true;
        }

        Map<String, AppProperties.CustomProviderProps> customProviders = 
            appProperties.getOauth2().getCustomProviders();
        
        if (customProviders != null && customProviders.containsKey(provider)) {
            return customProviders.get(provider).isAllowRegistration();
        }

        return true;
    }
}
