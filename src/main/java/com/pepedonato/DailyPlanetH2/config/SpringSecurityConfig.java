package com.pepedonato.DailyPlanetH2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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

    public SpringSecurityConfig(AccessDeniedHandler accessDeniedHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
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
                        .defaultSuccessUrl("/home", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
