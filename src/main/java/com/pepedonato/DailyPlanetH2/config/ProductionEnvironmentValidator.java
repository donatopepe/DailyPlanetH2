package com.pepedonato.DailyPlanetH2.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Verifica le variabili d'ambiente obbligatorie quando è attivo il profilo {@code prod},
 * prima che il contesto applicativo venga creato.
 *
 * <p>Senza questo controllo un {@code DB_URL} mancante produce messaggi poco chiari
 * ("Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DB_URL}"). Qui l'avvio si
 * interrompe con l'elenco esatto delle variabili da impostare: nessun default segreto e
 * nessun fallimento ambiguo.</p>
 */
public class ProductionEnvironmentValidator implements EnvironmentPostProcessor, Ordered {

    static final String PROFILE = "prod";
    static final String[] REQUIRED = {"DB_URL", "DB_USERNAME", "DB_PASSWORD", "DAILYPLANET_ADMIN_PASSWORD"};

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> mancanti = variabiliMancanti(environment);
        if (!mancanti.isEmpty()) {
            throw new IllegalStateException("Profilo 'prod': variabili d'ambiente obbligatorie mancanti: "
                    + String.join(", ", mancanti)
                    + ". Impostarle prima dell'avvio (nessun valore di default è previsto in produzione).");
        }
    }

    /** Ritorna l'elenco delle variabili obbligatorie non valorizzate (vuoto se non è il profilo prod). */
    List<String> variabiliMancanti(ConfigurableEnvironment environment) {
        List<String> mancanti = new ArrayList<>();
        boolean prod = List.of(environment.getActiveProfiles()).contains(PROFILE);
        if (!prod) {
            return mancanti;
        }
        for (String nome : REQUIRED) {
            if (valoreNonUtilizzabile(environment, nome)) {
                mancanti.add(nome);
            }
        }
        return mancanti;
    }

    /**
     * Un valore assente, vuoto, un placeholder non risolto o una risoluzione circolare
     * (proprietà definita come {@code ${SE_STESSA}}) contano tutti come "non impostata".
     */
    private boolean valoreNonUtilizzabile(ConfigurableEnvironment environment, String nome) {
        try {
            String valore = environment.getProperty(nome);
            return valore == null || valore.isBlank() || valore.startsWith("${");
        } catch (RuntimeException e) {
            return true;
        }
    }

    @Override
    public int getOrder() {
        // il più presto possibile, ma dopo la lettura dei file di configurazione
        return Ordered.LOWEST_PRECEDENCE;
    }
}
