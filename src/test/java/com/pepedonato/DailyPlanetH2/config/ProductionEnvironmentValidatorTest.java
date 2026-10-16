package com.pepedonato.DailyPlanetH2.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Verifica il controllo delle variabili d'ambiente obbligatorie in produzione.
 */
class ProductionEnvironmentValidatorTest {

    private final ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator();

    @Test
    @DisplayName("In sviluppo (nessun profilo prod) non viene richiesta alcuna variabile")
    void sviluppoNonRichiedeVariabili() {
        MockEnvironment environment = new MockEnvironment();

        assertTrue(validator.variabiliMancanti(environment).isEmpty());
    }

    @Test
    @DisplayName("In produzione elenca tutte le variabili mancanti")
    void produzioneElencaLeVariabiliMancanti() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        List<String> mancanti = validator.variabiliMancanti(environment);

        assertEquals(4, mancanti.size());
        assertTrue(mancanti.containsAll(List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD", "DAILYPLANET_ADMIN_PASSWORD")));
    }

    @Test
    @DisplayName("In produzione con variabili impostate non segnala nulla")
    void produzioneCompleta() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("DB_URL", "jdbc:postgresql://db:5432/dailyplanet");
        environment.setProperty("DB_USERNAME", "dailyplanet");
        environment.setProperty("DB_PASSWORD", "segreta");
        environment.setProperty("DAILYPLANET_ADMIN_PASSWORD", "amministratore-1");

        assertTrue(validator.variabiliMancanti(environment).isEmpty());
    }

    @Test
    @DisplayName("Un valore vuoto o un placeholder non risolto conta come mancante")
    void valoriVuotiOPlaceholder() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("DB_URL", "${DB_URL}");
        environment.setProperty("DB_USERNAME", "  ");
        environment.setProperty("DB_PASSWORD", "segreta");
        environment.setProperty("DAILYPLANET_ADMIN_PASSWORD", "amministratore-1");

        List<String> mancanti = validator.variabiliMancanti(environment);

        assertEquals(2, mancanti.size());
        assertTrue(mancanti.contains("DB_URL"));
        assertTrue(mancanti.contains("DB_USERNAME"));
        assertFalse(mancanti.contains("DB_PASSWORD"));
    }
}
