package com.pepedonato.DailyPlanetH2.config;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registrazione dei componenti della pipeline di sicurezza web. */
@Configuration
public class SecurityConfiguration {

    /**
     * Il rate limiter deve essere PRIMA di Spring Security, altrimenti i POST di login non
     * autenticati non raggiungono il filtro. La priorità esplicita evita la doppia
     * registrazione automatica del componente {@code @Component} nel servlet container.
     */
    @Bean
    public FilterRegistrationBean<Filter> loginRateLimitRegistration(LoginRateLimitFilter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setName("loginRateLimitFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

}
