package com.pepedonato.DailyPlanetH2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifica la gestione delle password: hash BCrypt in scrittura e verifica con
 * {@code matches} in lettura, cambio password, utenti disabilitati.
 */
@SpringBootTest
class MyUserDetailsServiceTest {

    private static final String RAW_PASSWORD = "password-robusta-1";

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

    private User newUser(String username, String rawPassword, String role, boolean disabled) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(rawPassword);
        user.setFirstname("Nome");
        user.setLastname("Cognome");
        user.setEmail(username + "@example.invalid");
        user.setUserrole(role);
        user.setDisabled(disabled);
        return user;
    }

    @Test
    @DisplayName("save() memorizza un hash BCrypt, mai la password in chiaro")
    void saveStoresBcryptHash() {
        assertTrue(userDetailsService.save(newUser("mario.rossi", RAW_PASSWORD, "USER", false)));

        User stored = userRepository.findByUsername("mario.rossi");
        assertNotNull(stored);
        assertNotEquals(RAW_PASSWORD, stored.getPassword());
        assertTrue(stored.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, stored.getPassword()));
    }

    @Test
    @DisplayName("loadUserByUsername restituisce l'hash memorizzato e le authorities corrette")
    void loadUserByUsernameReturnsStoredHash() {
        userDetailsService.save(newUser("admin.test", RAW_PASSWORD, "ADMIN", false));

        UserDetails details = userDetailsService.loadUserByUsername("admin.test");

        assertEquals("admin.test", details.getUsername());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, details.getPassword()));
        assertEquals("[ROLE_ADMIN]", details.getAuthorities().toString());
    }

    @Test
    @DisplayName("Utente inesistente o disabilitato: UsernameNotFoundException")
    void unknownOrDisabledUserIsRejected() {
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("non.esiste"));

        userDetailsService.save(newUser("utente.disabilitato", RAW_PASSWORD, "USER", true));
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("utente.disabilitato"));
    }

    @Test
    @DisplayName("loadUserByUsernameAndPassword valida con matches, non con confronto in chiaro")
    void loginWithUsernameAndPasswordUsesMatches() {
        userDetailsService.save(newUser("luigi.verdi", RAW_PASSWORD, "USER", false));

        assertNotNull(userDetailsService.loadUserByUsernameAndPassword("luigi.verdi", RAW_PASSWORD));
        assertNull(userDetailsService.loadUserByUsernameAndPassword("luigi.verdi", "password-sbagliata"));
        assertNull(userDetailsService.loadUserByUsernameAndPassword("non.esiste", RAW_PASSWORD));
    }

    @Test
    @DisplayName("ChangePassword richiede la password attuale e memorizza un nuovo hash")
    void changePasswordRequiresCurrentPassword() {
        userDetailsService.save(newUser("paola.bianchi", RAW_PASSWORD, "USER", false));
        String previousHash = userRepository.findByUsername("paola.bianchi").getPassword();

        assertFalse(userDetailsService.ChangePassword(
                "paola.bianchi", "password-errata", "nuova-password-9", "nuova-password-9"));
        assertEquals(previousHash, userRepository.findByUsername("paola.bianchi").getPassword());

        assertFalse(userDetailsService.ChangePassword(
                "paola.bianchi", RAW_PASSWORD, "nuova-password-9", "conferma-diversa"));
        assertEquals(previousHash, userRepository.findByUsername("paola.bianchi").getPassword());

        assertTrue(userDetailsService.ChangePassword(
                "paola.bianchi", RAW_PASSWORD, "nuova-password-9", "nuova-password-9"));

        String newHash = userRepository.findByUsername("paola.bianchi").getPassword();
        assertNotEquals(previousHash, newHash);
        assertTrue(passwordEncoder.matches("nuova-password-9", newHash));
        assertFalse(passwordEncoder.matches(RAW_PASSWORD, newHash));
    }

    @Test
    @DisplayName("AdminChangePassword imposta un hash valido e azzera mustChangePassword")
    void adminChangePasswordEncodesAndClearsFlag() {
        User user = newUser("carla.neri", RAW_PASSWORD, "ADMIN", false);
        user.setMustChangePassword(true);
        userDetailsService.save(user);
        assertTrue(userDetailsService.isPasswordChangeRequired("carla.neri"));

        assertTrue(userDetailsService.AdminChangePassword("carla.neri", "altra-password-7", "altra-password-7"));

        User reloaded = userRepository.findByUsername("carla.neri");
        assertTrue(passwordEncoder.matches("altra-password-7", reloaded.getPassword()));
        assertFalse(userDetailsService.isPasswordChangeRequired("carla.neri"));
        assertFalse(userDetailsService.AdminChangePassword("non.esiste", "altra-password-7", "altra-password-7"));
    }

    @Test
    @DisplayName("saveNoPw non modifica l'hash esistente")
    void saveNoPwKeepsPasswordHash() {
        userDetailsService.save(newUser("sara.gialli", RAW_PASSWORD, "USER", false));
        User stored = userRepository.findByUsername("sara.gialli");
        String hash = stored.getPassword();

        stored.setLastname("NuovoCognome");
        assertTrue(userDetailsService.saveNoPw(stored));

        User reloaded = userRepository.findByUsername("sara.gialli");
        assertEquals(hash, reloaded.getPassword());
        assertEquals("NuovoCognome", reloaded.getLastname());
    }
}
