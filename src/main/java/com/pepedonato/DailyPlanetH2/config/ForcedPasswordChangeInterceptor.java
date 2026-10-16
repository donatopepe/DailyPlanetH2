package com.pepedonato.DailyPlanetH2.config;

import com.pepedonato.DailyPlanetH2.service.MyUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Obbliga il cambio password quando l'account ha {@code mustChangePassword=true}
 * (tipicamente il primo accesso dell'amministratore creato dal seed).
 */
@Component
public class ForcedPasswordChangeInterceptor implements HandlerInterceptor {

    private final MyUserDetailsService userDetailsService;

    public ForcedPasswordChangeInterceptor(MyUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        if (Boolean.TRUE.equals(userDetailsService.isPasswordChangeRequired(authentication.getName()))) {
            response.sendRedirect(request.getContextPath() + "/user/changepw");
            return false;
        }
        return true;
    }
}
