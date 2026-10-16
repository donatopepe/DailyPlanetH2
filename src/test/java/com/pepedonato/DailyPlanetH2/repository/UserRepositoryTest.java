package com.pepedonato.DailyPlanetH2.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pepedonato.DailyPlanetH2.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifica la mappatura JPA dell'utente e i vincoli del database.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("$2a$10$hashfintoperiltest0000000000000000000000000000000000000");
        user.setFirstname("Nome");
        user.setLastname("Cognome");
        user.setEmail(email);
        user.setUserrole("USER");
        user.setDisabled(false);
        return user;
    }

    @Test
    @DisplayName("Salvataggio e ricerca per username (tabella 'users', non riservata)")
    void saveAndFindByUsername() {
        userRepository.saveAndFlush(user("mario.rossi", "mario@example.invalid"));

        User found = userRepository.findByUsername("mario.rossi");

        assertNotNull(found);
        assertEquals("mario@example.invalid", found.getEmail());
        assertEquals("USER", found.getUserrole());
        assertFalse(Boolean.TRUE.equals(found.getMustChangePassword()));
        assertNull(userRepository.findByUsername("inesistente"));
    }

    @Test
    @DisplayName("Il flag mustChangePassword viene persistito")
    void mustChangePasswordIsPersisted() {
        User admin = user("amministratore", "admin@example.invalid");
        admin.setMustChangePassword(true);
        userRepository.saveAndFlush(admin);

        assertTrue(Boolean.TRUE.equals(userRepository.findByUsername("amministratore").getMustChangePassword()));
    }

    @Test
    @DisplayName("Username duplicato rifiutato dal vincolo unique")
    void duplicateUsernameIsRejected() {
        userRepository.saveAndFlush(user("duplicato", "primo@example.invalid"));

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepository.saveAndFlush(user("duplicato", "secondo@example.invalid")));
    }
}
