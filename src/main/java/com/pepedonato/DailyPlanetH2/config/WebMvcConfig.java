package com.pepedonato.DailyPlanetH2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registrazione degli interceptor dell'applicazione.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ForcedPasswordChangeInterceptor forcedPasswordChangeInterceptor;

    public WebMvcConfig(ForcedPasswordChangeInterceptor forcedPasswordChangeInterceptor) {
        this.forcedPasswordChangeInterceptor = forcedPasswordChangeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(forcedPasswordChangeInterceptor)
                .addPathPatterns("/user/**", "/admin/**")
                .excludePathPatterns("/user/changepw", "/user/doChangepw", "/logout");
    }
}
