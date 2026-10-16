package com.pepedonato.DailyPlanetH2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Hardening di sicurezza web: header browser difensivi e logging request limitato ai
 * metadati (mai body, query string o header di autenticazione).
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * Il filtro resta attivo ma non emette request, body, query, header o session ID.
     * Le righe di request contengono spesso dati personali e possono facilitare
     * l'enumerazione; l'audit applicativo registra solo esiti aggregati e errori tecnici.
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(false);
        filter.setIncludeQueryString(false);
        filter.setIncludePayload(false);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("HTTP request suppressed: ");
        return filter;
    }
}
