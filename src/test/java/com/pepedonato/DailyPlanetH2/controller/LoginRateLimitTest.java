package com.pepedonato.DailyPlanetH2.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

/** Verifica che i tentativi ripetuti falliscano temporaneamente senza bloccare globalmente l'account. */
@SpringBootTest
@AutoConfigureMockMvc
class LoginRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Dopo otto login falliti, il successivo è limitato con HTTP 429 e Retry-After")
    void bloccaDopoOttoTentativi() throws Exception {
        String usernameUnico = "rate.limit." + System.nanoTime();
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(post("/login")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("username", usernameUnico)
                            .param("password", "password-errata"))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        if (code != 302 && code != 429) {
                            throw new AssertionError("attesi 302 o 429, ricevuto " + code);
                        }
                    });
        }

        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", usernameUnico)
                        .param("password", "password-errata"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
