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

    public LoginRateLimitFilter(LoginFailureTracker tracker) {
        this.tracker = tracker;
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
        String key = key(request.getRemoteAddr(), username);
        if (tracker.isBlocked(key)) {
            response.setHeader("Retry-After", Long.toString(tracker.retryAfterSeconds(key)));
            response.sendError(429,
                    "Troppi tentativi di accesso. Attendere prima di riprovare.");
            return;
        }
        chain.doFilter(request, response);
    }

    static String key(String remoteAddress, String username) {
        String user = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return (remoteAddress == null ? "unknown" : remoteAddress) + "|" + user;
    }


    /** Se l'autenticazione fallisce, l'AuthenticationFailureHandler registra solo la chiave. */
    public void failed(String remoteAddress, String username) {
        tracker.registerFailure(key(remoteAddress, username));
    }

    public void succeeded(String remoteAddress, String username) {
        tracker.reset(key(remoteAddress, username));
    }
}
