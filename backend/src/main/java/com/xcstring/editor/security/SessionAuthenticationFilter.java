package com.xcstring.editor.security;

import com.xcstring.editor.entity.User;
import com.xcstring.editor.service.SessionService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter implements Filter {
    private final SessionService sessionService;
    private static final String CURRENT_USER_ATTR = "currentUser";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            Optional<User> user = sessionService.getCurrentUser(httpRequest);
            user.ifPresent(u -> httpRequest.setAttribute(CURRENT_USER_ATTR, u));
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    public static User getCurrentUser(HttpServletRequest request) {
        User user = getOptionalUser(request).orElse(null);
        if (user == null) {
            throw new RuntimeException("Authentication required");
        }
        return user;
    }

    public static Optional<User> getOptionalUser(HttpServletRequest request) {
        Object attr = request.getAttribute(CURRENT_USER_ATTR);
        if (attr instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
