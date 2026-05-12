package com.xcstring.editor.controller;

import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.entity.User;
import com.xcstring.editor.oauth2.OAuth2ProviderFactory;
import com.xcstring.editor.security.SessionAuthenticationFilter;
import com.xcstring.editor.service.AIService;
import com.xcstring.editor.service.AuthService;
import com.xcstring.editor.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backend/index.php")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final OAuth2ProviderFactory oauth2ProviderFactory;
    private final AIService aiService;
    private final AppProperties appProperties;

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String name = (String) body.get("name");
        String password = (String) body.get("password");
        String inviteToken = (String) body.get("invite_token");

        if (email == null || email.isEmpty() || name == null || name.isEmpty() || password == null || password.isEmpty()) {
            throw new RuntimeException("Email, name, and password are required");
        }

        Long userId = authService.register(email, name, password, inviteToken);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("user_id", userId);
        return response;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body, HttpServletResponse response) {
        String email = (String) body.get("email");
        String password = (String) body.get("password");

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new RuntimeException("Email and password are required");
        }

        Map<String, Object> user = authService.login(email, password, response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("user", user);
        return result;
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.logout(request, response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/auth/invites/create")
    public Map<String, Object> createInvite(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User currentUser = SessionAuthenticationFilter.getCurrentUser(request);
        String email = (String) body.get("email");

        Map<String, String> invite = authService.createInvite(currentUser.getId(), email);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("invite", invite);
        return result;
    }

    @GetMapping("/auth/user")
    public Map<String, Object> getCurrentUser(HttpServletRequest request) {
        User currentUser = SessionAuthenticationFilter.getOptionalUser(request).orElse(null);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("registration_enabled", appProperties.getRegistration().isEnabled());
        config.put("oauth2_enabled", appProperties.getOauth2().isEnabled());
        
        Map<String, Map<String, Object>> oauth2Providers = oauth2ProviderFactory.getAvailableProviders(appProperties);
        config.put("oauth2_providers", new ArrayList<>(oauth2Providers.values()));
        
        config.put("ai_enabled", aiService.isEnabled());
        config.put("ai_providers", aiService.getAvailableProviders());
        
        boolean canCreateInvites = currentUser != null && authService.canCreateInvites(currentUser.getEmail());
        config.put("can_create_invites", canCreateInvites);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        
        if (currentUser != null) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", currentUser.getId());
            userMap.put("email", currentUser.getEmail());
            userMap.put("name", currentUser.getName());
            userMap.put("avatar_url", currentUser.getAvatarUrl());
            result.put("user", userMap);
        } else {
            result.put("user", null);
        }
        
        result.put("config", config);
        return result;
    }

    @GetMapping("/auth/invites/my")
    public Map<String, Object> getMyInvites(HttpServletRequest request) {
        User currentUser = SessionAuthenticationFilter.getCurrentUser(request);

        List<Map<String, String>> invites = authService.getUserInvites(currentUser.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("invites", invites);
        return result;
    }

    @GetMapping("/auth/invites/validate/{token}")
    public Map<String, Object> validateInviteToken(
            @PathVariable String token,
            @RequestParam(required = false) String email) {
        
        boolean valid = authService.validateInviteToken(token, email);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("valid", valid);
        return result;
    }

    @DeleteMapping("/auth/invites/{inviteId}")
    public Map<String, Object> revokeInvite(@PathVariable Long inviteId, HttpServletRequest request) {
        User currentUser = SessionAuthenticationFilter.getCurrentUser(request);

        authService.revokeInvite(inviteId, currentUser.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }
}
