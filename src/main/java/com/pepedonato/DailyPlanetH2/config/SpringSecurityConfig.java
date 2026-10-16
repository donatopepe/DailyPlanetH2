package com.pepedonato.DailyPlanetH2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Configurazione di sicurezza (Spring Security 6 / Spring Boot 3).
 *
 * Regole di accesso:
 * <ul>
 *   <li>pagine pubbliche: {@code /}, {@code /home}, {@code /about}, {@code /login}, errori e risorse statiche;</li>
 *   <li>{@code /admin/**} richiede l'autorità {@code ROLE_ADMIN};</li>
 *   <li>{@code /user/**} richiede {@code ROLE_ADMIN} o {@code ROLE_USER};</li>
 *   <li>403 gestito da {@link com.pepedonato.DailyPlanetH2.error.MyAccessDeniedHandler}.</li>
 * </ul>
 *
 * La codifica delle password è BCrypt; {@code MyUserDetailsService} è il
 * {@code UserDetailsService} usato dal provider di default di Spring Security.
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    private final AccessDeniedHandler accessDeniedHandler;
    private final LoginRateLimitFilter loginRateLimitFilter;

    public SpringSecurityConfig(AccessDeniedHandler accessDeniedHandler, LoginRateLimitFilter loginRateLimitFilter) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.loginRateLimitFilter = loginRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF attivo (default Spring Security 6): i form Thymeleaf generati con
                // th:action includono automaticamente il token; le chiamate AJAX lo
                // prelevano dal meta tag del layout.
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/home", "/about", "/login", "/403", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        // endpoint di salute/informazioni per sonde e monitoraggio
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/user/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler())
                        .failureHandler(loginFailureHandler())
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false))
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                // Header di difesa lato browser: niente sniffing MIME, framing o referrer
                // completo su origini diverse. HSTS è attivo: prod va servito in HTTPS.
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> { })
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'")))
                .exceptionHandling(exception -> exception.accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            loginRateLimitFilter.succeeded(request, username);
            response.sendRedirect(request.getContextPath() + "/user");
        };
    }

    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            loginRateLimitFilter.failed(request, username);
            if (loginRateLimitFilter.isBlocked(request, username)) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(loginRateLimitFilter.retryAfterSeconds(request, username)));
                request.getRequestDispatcher("/login?error=rate-limit").forward(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?error");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
