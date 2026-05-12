package com.xcstring.editor.service;

import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.entity.Session;
import com.xcstring.editor.entity.User;
import com.xcstring.editor.repository.SessionRepository;
import com.xcstring.editor.util.SecureTokenGenerator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    private final SessionRepository sessionRepository;
    private final AppProperties properties;

    @Transactional
    public String createSession(User user, HttpServletResponse response) {
        String sessionId = SecureTokenGenerator.generate(32);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(properties.getSession().getLifetime());
        
        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);
        session.setExpiresAt(expiresAt);
        sessionRepository.save(session);
        
        Cookie cookie = new Cookie(properties.getSession().getCookieName(), sessionId);
        cookie.setMaxAge(properties.getSession().getLifetime());
        cookie.setPath("/");
        cookie.setHttpOnly(properties.getSession().isCookieHttpOnly());
        cookie.setSecure(properties.getSession().isCookieSecure());
        response.addCookie(cookie);
        
        return sessionId;
    }

    @Transactional(readOnly = true)
    public Optional<User> getCurrentUser(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId == null) return Optional.empty();
        
        return sessionRepository.findValidUserBySessionId(sessionId, LocalDateTime.now());
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId != null) {
            sessionRepository.deleteById(sessionId);
        }
        Cookie cookie = new Cookie(properties.getSession().getCookieName(), "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(properties.getSession().getCookieName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanExpiredSessions() {
        sessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }
}
