package com.xcstring.editor.controller;

import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.entity.User;
import com.xcstring.editor.security.SessionAuthenticationFilter;
import com.xcstring.editor.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DebugController {

    private final AuthService authService;
    private final AppProperties appProperties;

    @GetMapping("/debug/invites")
    public Map<String, Object> debugInvites(HttpServletRequest request) {
        User currentUser = SessionAuthenticationFilter.getOptionalUser(request).orElse(null);

        List<String> inviteDomains = appProperties.getRegistration().getInviteDomains();
        boolean inviteDomainsConfigured = inviteDomains != null && !inviteDomains.isEmpty();

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("invite_domains_configured", inviteDomainsConfigured);
        debug.put("invite_domains", inviteDomains != null ? inviteDomains : List.of());
        debug.put("registration_enabled", appProperties.getRegistration().isEnabled());

        if (currentUser != null) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", currentUser.getId());
            userMap.put("email", currentUser.getEmail());
            userMap.put("name", currentUser.getName());
            debug.put("current_user", userMap);
        } else {
            debug.put("current_user", null);
        }

        boolean canCreateInvites = currentUser != null && authService.canCreateInvites(currentUser.getEmail());
        debug.put("can_create_invites", canCreateInvites);

        if (currentUser != null) {
            String email = currentUser.getEmail();
            String userDomain = email.contains("@") ? email.substring(email.lastIndexOf("@") + 1) : "";
            debug.put("user_domain", userDomain);

            try {
                List<Map<String, String>> existingInvites = authService.getUserInvites(currentUser.getId());
                debug.put("existing_invites", existingInvites);
            } catch (Exception e) {
                debug.put("existing_invites", List.of());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("debug", debug);
        return result;
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "XCString Tool API is working");
        return result;
    }
}
