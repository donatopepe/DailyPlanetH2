package com.pepedonato.DailyPlanetH2.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import com.pepedonato.DailyPlanetH2.service.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica il seed dell'account amministratore: password da configurazione,
 * hash BCrypt, obbligo di cambio password, idempotenza e blocco in produzione.
 */
@SpringBootTest(properties = "dailyplanet.admin.password=seme-iniziale-1")
@Transactional
class AdminAccountInitializerTest {

    @Autowired
    private AdminAccountInitializer initializer;

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Crea l'amministratore con hash BCrypt e mustChangePassword=true")
    void createsAdministratorWithHashedPassword() {
        initializer.run(null);

        User admin = userRepository.findByUsername("administrator");

        assertNotNull(admin);
        assertEquals("ADMIN", admin.getUserrole());
        assertTrue(Boolean.TRUE.equals(admin.getMustChangePassword()));
        assertFalse(Boolean.TRUE.equals(admin.getDisabled()));
        assertTrue(passwordEncoder.matches("seme-iniziale-1", admin.getPassword()));
        assertFalse("seme-iniziale-1".equals(admin.getPassword()));
    }

    @Test
    @DisplayName("Seed idempotente: esecuzioni ripetute non duplicano l'account")
    void seedIsIdempotent() {
        initializer.run(null);
        initializer.run(null);
        initializer.run(null);

        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    @DisplayName("In produzione la password dell'admin è obbligatoria: nessun default sicuro")
    void failsFastInProductionWithoutPassword() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        MyUserDetailsService service = mock(MyUserDetailsService.class);

        AdminAccountInitializer prodInitializer =
                new AdminAccountInitializer(service, environment, "administrator", "a@example.invalid", "");

        assertThrows(IllegalStateException.class, () -> prodInitializer.run(null));
    }

    @Test
    @DisplayName("Senza password (fuori produzione) non crea account e non fallisce")
    void skipsCreationWithoutPassword() {
        MockEnvironment environment = new MockEnvironment();
        MyUserDetailsService service = mock(MyUserDetailsService.class);
        when(service.existsUserByUsername(any())).thenReturn(false);

        AdminAccountInitializer devInitializer =
                new AdminAccountInitializer(service, environment, "administrator", "a@example.invalid", null);

        devInitializer.run(null);

        assertEquals(0, userRepository.findAll().size());
        verify(service, never()).save(any());
    }
}
