package com.pepedonato.DailyPlanetH2.config;

import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.service.MyUserDetailsService;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Crea l'account amministratore al primo avvio, in modo idempotente.
 *
 * <p>Sostituisce la creazione che avveniva nella pagina di login con credenziali
 * scritte nel codice. La password proviene dalla variabile d'ambiente
 * {@code DAILYPLANET_ADMIN_PASSWORD} (proprietà {@code dailyplanet.admin.password}):
 * nessun default segreto. In produzione l'assenza della variabile blocca l'avvio, in
 * sviluppo si usa il default documentato del profilo {@code dev}.</p>
 *
 * <p>L'account creato ha {@code mustChangePassword=true}: al primo accesso l'utente è
 * obbligato a cambiare la password.</p>
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final MyUserDetailsService userDetailsService;
    private final Environment environment;

    private final String username;
    private final String email;
    private final String password;

    public AdminAccountInitializer(MyUserDetailsService userDetailsService,
                                   Environment environment,
                                   @Value("${dailyplanet.admin.username:administrator}") String username,
                                   @Value("${dailyplanet.admin.email:admin@example.invalid}") String email,
                                   @Value("${dailyplanet.admin.password:}") String password) {
        this.userDetailsService = userDetailsService;
        this.environment = environment;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (password == null || password.isBlank()) {
            if (production) {
                throw new IllegalStateException(
                        "DAILYPLANET_ADMIN_PASSWORD non impostata: in produzione la password "
                        + "dell'account amministratore è obbligatoria (nessun default sicuro).");
            }
            log.warn("dailyplanet.admin.password non impostata: nessun account amministratore creato. "
                     + "Impostare DAILYPLANET_ADMIN_PASSWORD per crearlo.");
            return;
        }

        if (userDetailsService.existsUserByUsername(username)) {
            log.info("Account amministratore '{}' già presente: nessuna azione.", username);
            return;
        }

        if (password.length() < 8) {
            log.warn("dailyplanet.admin.password è più corta di 8 caratteri: usare una password più robusta.");
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(password);          // codificata in MyUserDetailsService.save()
        admin.setFirstname("Amministratore");
        admin.setLastname("DailyPlanet");
        admin.setEmail(email);
        admin.setUserrole("ADMIN");
        admin.setDisabled(false);
        admin.setMustChangePassword(true);

        if (Boolean.TRUE.equals(userDetailsService.save(admin))) {
            log.info("Account amministratore '{}' creato: al primo accesso è richiesto il cambio password.",
                     username);
        } else {
            log.error("Creazione dell'account amministratore '{}' non riuscita.", username);
        }
    }
}
