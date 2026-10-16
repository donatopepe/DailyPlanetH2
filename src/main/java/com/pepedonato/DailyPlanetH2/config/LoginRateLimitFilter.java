package com.pepedonato.DailyPlanetH2.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rate limit dedicato alla rotta di login, senza mai registrare password o token CSRF. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/login";
    private final LoginFailureTracker tracker;
    private final org.springframework.core.env.Environment environment;

    public LoginRateLimitFilter(LoginFailureTracker tracker, org.springframework.core.env.Environment environment) {
        this.tracker = tracker;
        this.environment = environment;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !LOGIN_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String clientAddress = clientAddress(request);
        String key = key(clientAddress, username);
        if (tracker.isBlocked(key)) {
            response.setHeader("Retry-After", Long.toString(tracker.retryAfterSeconds(key)));
            response.sendError(429,
                    "Troppi tentativi di accesso. Attendere prima di riprovare.");
            return;
        }
        chain.doFilter(request, response);
    }

    public void failed(HttpServletRequest request, String username) {
        tracker.registerFailure(key(clientAddress(request), username));
    }

    public void succeeded(HttpServletRequest request, String username) {
        tracker.reset(key(clientAddress(request), username));
    }

    public boolean isBlocked(HttpServletRequest request, String username) {
        return tracker.isBlocked(key(clientAddress(request), username));
    }

    public long retryAfterSeconds(HttpServletRequest request, String username) {
        return tracker.retryAfterSeconds(key(clientAddress(request), username));
    }

    private String clientAddress(HttpServletRequest request) {
        // X-Forwarded-For è fidato solo dietro un proxy esplicitamente configurato.
        // Fuori da tale scenario un client potrebbe falsificarlo e ruotare i bucket.
        boolean trustedProxy = environment.getProperty("dailyplanet.security.trust-forwarded-headers",
                Boolean.class, false);
        if (trustedProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    static String key(String remoteAddress, String username) {
        String user = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return (remoteAddress == null ? "unknown" : remoteAddress) + "|" + user;
    }


}
