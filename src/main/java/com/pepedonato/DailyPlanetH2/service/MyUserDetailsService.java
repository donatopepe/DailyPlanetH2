package com.pepedonato.DailyPlanetH2.service;

import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servizio utenti: autenticazione, gestione account e cambio password.
 *
 * <p>Regola di sicurezza applicata qui: la password è <strong>sempre</strong> memorizzata
 * come hash BCrypt e verificata con {@link PasswordEncoder#matches(CharSequence, String)}.
 * Non esiste alcun confronto in chiaro e nessuna password viene scritta nei log.</p>
 *
 * @author dr.ing. Pepe Donato
 */
@Service
public class MyUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(MyUserDetailsService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public MyUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("invalid-login-placeholder");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            log.debug("Utente non trovato: {}", username);
            throw new UsernameNotFoundException("Utente non trovato: " + username);
        }
        if (Boolean.TRUE.equals(user.getDisabled())) {
            log.info("Tentativo di accesso con utente disabilitato: {}", username);
            throw new UsernameNotFoundException("Utente disabilitato: " + username);
        }
        // L'hash memorizzato in database viene passato cosi' com'e': la verifica
        // (matches) e' compito di DaoAuthenticationProvider/PasswordEncoder.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getUserrole()))
                .disabled(Boolean.TRUE.equals(user.getDisabled()))
                .build();
    }

    /**
     * Ritorna l'utente solo se username e password corrispondono.
     *
     * @return l'utente autenticato oppure {@code null}
     */
    public User loadUserByUsernameAndPassword(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null || Boolean.TRUE.equals(user.getDisabled())) {
            // Mantiene il costo temporale dell'operazione anche per username assenti/disabilitati,
            // riducendo il rischio di enumerazione degli account (nessuna password viene loggata).
            passwordEncoder.matches("invalid-login-placeholder", dummyPasswordHash);
            log.debug("Autenticazione fallita per l'utente: {}", username);
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.debug("Password errata per l'utente: {}", username);
            return null;
        }
        return user;
    }

    public User loadUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + id));
    }

    /**
     * Indica se all'utente è richiesto il cambio password al primo accesso.
     */
    public Boolean isPasswordChangeRequired(String username) {
        User user = userRepository.findByUsername(username);
        return user != null && Boolean.TRUE.equals(user.getMustChangePassword());
    }

    /**
     * Cambio password dell'utente autenticato: richiede la password attuale.
     */
    public Boolean ChangePassword(String username, String password, String new_password, String confirm_password) {
        User user = loadUserByUsernameAndPassword(username, password);
        if (user == null) {
            return false;
        }
        if (new_password == null || !new_password.equals(confirm_password)) {
            log.debug("Le nuove password non coincidono per l'utente: {}", username);
            return false;
        }
        user.setPassword(passwordEncoder.encode(new_password));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password aggiornata per l'utente: {}", username);
        return true;
    }

    /**
     * Cambio password eseguito da un amministratore (non richiede la password attuale).
     */
    public Boolean AdminChangePassword(String username, String new_password, String confirm_password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            log.debug("Utente non trovato: {}", username);
            return false;
        }
        if (new_password == null || !new_password.equals(confirm_password)) {
            log.debug("Le nuove password non coincidono per l'utente: {}", username);
            return false;
        }
        user.setPassword(passwordEncoder.encode(new_password));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password reimpostata dall'amministratore per l'utente: {}", username);
        return true;
    }

    /**
     * Crea o aggiorna un account codificando la password indicata.
     * Da usare solo quando {@code user.getPassword()} contiene una password in chiaro.
     */
    public Boolean save(User user) {
        try {
            // Regola di sicurezza: ogni account creato da zero (seed o area amministratore)
            // deve cambiare la password al primo accesso.
            if (user.getId() == null) {
                user.setMustChangePassword(Boolean.TRUE);
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            log.info("Account salvato: {}", user.getUsername());
            return true;
        } catch (RuntimeException e) {
            log.warn("Account non salvato: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Crea o aggiorna un account senza toccare la password (che è già un hash).
     */
    public Boolean saveNoPw(User user) {
        try {
            userRepository.save(user);
            log.info("Account aggiornato: {}", user.getUsername());
            return true;
        } catch (RuntimeException e) {
            log.warn("Account non aggiornato: {}", user.getUsername(), e);
            return false;
        }
    }

    public Boolean existsUserByUsername(String username) {
        return userRepository.findByUsername(username) != null;
    }

    public List<User> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "lastname"));
    }
}
