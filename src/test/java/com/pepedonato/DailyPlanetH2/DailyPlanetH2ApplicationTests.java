package com.pepedonato.DailyPlanetH2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica che il contesto Spring Boot si avvii con la configurazione di default
 * (profilo di test, database H2 in memoria).
 */
@SpringBootTest
class DailyPlanetH2ApplicationTests {

    @Test
    void contextLoads() {
        // il test passa se il contesto applicativo si avvia senza errori
    }
}
