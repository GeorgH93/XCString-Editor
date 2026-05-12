package com.xcstring.editor.service;

import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.entity.FileShare;
import com.xcstring.editor.entity.Invite;
import com.xcstring.editor.entity.OAuth2Account;
import com.xcstring.editor.entity.OAuth2State;
import com.xcstring.editor.entity.PendingShare;
import com.xcstring.editor.entity.User;
import com.xcstring.editor.repository.FileShareRepository;
import com.xcstring.editor.repository.InviteRepository;
import com.xcstring.editor.repository.OAuth2AccountRepository;
import com.xcstring.editor.repository.OAuth2StateRepository;
import com.xcstring.editor.repository.PendingShareRepository;
import com.xcstring.editor.repository.UserRepository;
import com.xcstring.editor.util.SecureTokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final OAuth2AccountRepository oauth2AccountRepository;
    private final OAuth2StateRepository oauth2StateRepository;
    private final PendingShareRepository pendingShareRepository;
    private final FileShareRepository fileShareRepository;
    private final SessionService sessionService;
    private final AppProperties properties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public Long register(String email, String name, String password, String inviteToken) {
        if (!isEmailAllowed(email)) {
            throw new RuntimeException("Email domain not allowed");
        }

        if (!properties.getRegistration().isEnabled()) {
            if (inviteToken == null || inviteToken.isEmpty()) {
                throw new RuntimeException("Registration is disabled");
            }
            if (!validateInviteToken(inviteToken, email)) {
                throw new RuntimeException("Invalid or expired invite token");
            }
        }

        if (email == null || !email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new RuntimeException("Invalid email address");
        }

        if (password == null || password.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name is required");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        User user = new User();
        user.setEmail(email);
        user.setName(name.trim());
        user.setPasswordHash(passwordHash);
        userRepository.save(user);

        Long userId = user.getId();

        if (inviteToken != null && !inviteToken.isEmpty()) {
            markInviteAsUsed(inviteToken, userId);
        }

        convertPendingSharesForNewUser(email, userId);

        return userId;
    }

    @Transactional
    public Map<String, Object> login(String email, String password, HttpServletResponse response) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        sessionService.createSession(user, response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("name", user.getName());
        result.put("avatar_url", user.getAvatarUrl());
        return result;
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.logout(request, response);
    }

    @Transactional
    public Map<String, Object> handleOAuth2Login(Map<String, Object> userInfo, String providerName, boolean allowRegistration) {
        String providerId = (String) userInfo.get("provider_id");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String avatar = (String) userInfo.get("avatar");

        Optional<OAuth2Account> oauthAccountOpt = oauth2AccountRepository.findByProviderAndProviderUserId(providerName, providerId);

        if (oauthAccountOpt.isPresent()) {
            OAuth2Account oauthAccount = oauthAccountOpt.get();
            User user = oauthAccount.getUser();

            if (user == null) {
                throw new RuntimeException("User account not found");
            }

            updateUserFromOAuth2(user, name, avatar);

            return buildUserResponse(user);
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            linkOAuth2Account(existingUser.getId(), providerName, providerId);
            updateUserFromOAuth2(existingUser, name, avatar);

            return buildUserResponse(existingUser);
        }

        return createUserFromOAuth2(userInfo, providerName, allowRegistration);
    }

    @Transactional(readOnly = true)
    public boolean canCreateInvites(String userEmail) {
        List<String> inviteDomains = properties.getRegistration().getInviteDomains();

        if (inviteDomains == null || inviteDomains.isEmpty()) {
            return false;
        }

        String domain = extractDomain(userEmail);
        return inviteDomains.contains(domain);
    }

    @Transactional
    public Map<String, String> createInvite(Long creatorUserId, String email) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new RuntimeException("Creator user not found"));

        if (!canCreateInvites(creator.getEmail())) {
            throw new RuntimeException("You are not authorized to create invites");
        }

        String token = SecureTokenGenerator.generate(32);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

        Invite invite = new Invite();
        invite.setToken(token);
        invite.setCreatedByUser(creator);
        invite.setEmail(email);
        invite.setExpiresAt(expiresAt);
        inviteRepository.save(invite);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("expires_at", expiresAt.format(DATE_FORMATTER));
        result.put("email", email);
        return result;
    }

    @Transactional(readOnly = true)
    public boolean validateInviteToken(String token, String email) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        Optional<Invite> inviteOpt = inviteRepository.findByToken(token);

        if (inviteOpt.isEmpty()) {
            return false;
        }

        Invite invite = inviteOpt.get();

        if (invite.getUsedAt() != null) {
            return false;
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (invite.getEmail() != null && !invite.getEmail().isEmpty() && !invite.getEmail().equals(email)) {
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getUserInvites(Long userId) {
        List<Invite> invites = inviteRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, String>> result = new ArrayList<>();

        for (Invite invite : invites) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("id", String.valueOf(invite.getId()));
            map.put("token", invite.getToken());
            map.put("email", invite.getEmail());
            map.put("expires_at", invite.getExpiresAt() != null ? invite.getExpiresAt().format(DATE_FORMATTER) : null);
            map.put("used_at", invite.getUsedAt() != null ? invite.getUsedAt().format(DATE_FORMATTER) : null);
            map.put("created_at", invite.getCreatedAt() != null ? invite.getCreatedAt().format(DATE_FORMATTER) : null);
            map.put("used_by_name", invite.getUsedByUser() != null ? invite.getUsedByUser().getName() : null);
            result.add(map);
        }

        return result;
    }

    @Transactional
    public void revokeInvite(Long inviteId, Long userId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        if (!invite.getCreatedByUser().getId().equals(userId)) {
            throw new RuntimeException("You can only revoke your own invites");
        }

        if (invite.getUsedAt() != null) {
            throw new RuntimeException("Cannot revoke an invite that has already been used");
        }

        inviteRepository.delete(invite);
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getUserOAuth2Providers(Long userId) {
        List<OAuth2Account> accounts = oauth2AccountRepository.findByUserId(userId);
        List<Map<String, String>> result = new ArrayList<>();

        for (OAuth2Account account : accounts) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("provider", account.getProvider());
            map.put("created_at", account.getCreatedAt() != null ? account.getCreatedAt().format(DATE_FORMATTER) : null);
            result.add(map);
        }

        return result;
    }

    @Transactional
    public void cleanExpiredSessions() {
        sessionService.cleanExpiredSessions();
    }

    @Transactional
    public void cleanExpiredInvites() {
        inviteRepository.deleteExpiredInvites(LocalDateTime.now());
    }

    @Transactional
    public String generateOAuth2State(String provider) {
        String state = SecureTokenGenerator.generate(32);

        OAuth2State oauth2State = new OAuth2State();
        oauth2State.setId(state);
        oauth2State.setProvider(provider);
        oauth2StateRepository.save(oauth2State);

        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        oauth2StateRepository.deleteOlderThan(cutoff);

        return state;
    }

    @Transactional
    public boolean verifyOAuth2State(String state, String provider) {
        Optional<OAuth2State> stateOpt = oauth2StateRepository.findByIdAndProvider(state, provider);

        if (stateOpt.isPresent()) {
            oauth2StateRepository.deleteById(state);
            return true;
        }

        return false;
    }

    private boolean isEmailAllowed(String email) {
        List<String> allowedDomains = properties.getRegistration().getAllowedDomains();

        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return true;
        }

        String domain = extractDomain(email);
        return allowedDomains.contains(domain);
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(email.lastIndexOf("@") + 1);
    }

    private void markInviteAsUsed(String token, Long userId) {
        Optional<Invite> inviteOpt = inviteRepository.findByToken(token);
        if (inviteOpt.isPresent()) {
            Invite invite = inviteOpt.get();
            User usedByUser = userRepository.getReferenceById(userId);
            invite.setUsedByUser(usedByUser);
            invite.setUsedAt(LocalDateTime.now());
            inviteRepository.save(invite);
        }
    }

    @Transactional
    public void convertPendingSharesForNewUser(String email, Long userId) {
        try {
            List<PendingShare> pendingShares = pendingShareRepository.findBySharedWithEmail(email);
            User newUser = userRepository.getReferenceById(userId);

            for (PendingShare pending : pendingShares) {
                FileShare fileShare = new FileShare();
                fileShare.setFile(pending.getFile());
                fileShare.setSharedWithUser(newUser);
                fileShare.setCanEdit(pending.getCanEdit() != null ? pending.getCanEdit() : false);
                fileShareRepository.save(fileShare);

                pendingShareRepository.delete(pending);
            }
        } catch (Exception e) {
            log.error("Failed to convert pending shares for new user {}: {}", email, e.getMessage());
        }
    }

    private void linkOAuth2Account(Long userId, String provider, String providerUserId) {
        Optional<OAuth2Account> existingOpt = oauth2AccountRepository.findByProviderAndProviderUserId(provider, providerUserId);

        User user = userRepository.getReferenceById(userId);

        if (existingOpt.isPresent()) {
            OAuth2Account existing = existingOpt.get();
            existing.setUser(user);
            oauth2AccountRepository.save(existing);
        } else {
            OAuth2Account account = new OAuth2Account();
            account.setUser(user);
            account.setProvider(provider);
            account.setProviderUserId(providerUserId);
            oauth2AccountRepository.save(account);
        }
    }

    private void updateUserFromOAuth2(User user, String name, String avatar) {
        user.setName(name);
        user.setAvatarUrl(avatar);
        userRepository.save(user);
    }

    private Map<String, Object> createUserFromOAuth2(Map<String, Object> userInfo, String providerName, boolean allowRegistration) {
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String avatar = (String) userInfo.get("avatar");
        String providerId = (String) userInfo.get("provider_id");

        if (!isEmailAllowed(email)) {
            throw new RuntimeException("Email domain not allowed");
        }

        if (!allowRegistration) {
            throw new RuntimeException("Registration is disabled");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setAvatarUrl(avatar);
        userRepository.save(user);

        linkOAuth2Account(user.getId(), providerName, providerId);

        convertPendingSharesForNewUser(email, user.getId());

        return buildUserResponse(user);
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("name", user.getName());
        result.put("avatar_url", user.getAvatarUrl());
        return result;
    }
}
