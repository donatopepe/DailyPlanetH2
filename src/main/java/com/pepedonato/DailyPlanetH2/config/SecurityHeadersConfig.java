package com.pepedonato.DailyPlanetH2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Hardening di sicurezza web: header browser difensivi e logging request limitato ai
 * metadati (mai body, query string o header di autenticazione).
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * Registra solo metodo, URI e IP remoto: non includere body/header/query perché possono
     * contenere password, token CSRF, cookie di sessione o dati personali.
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(false);
        filter.setIncludePayload(false);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("HTTP request: ");
        return filter;
    }
}
