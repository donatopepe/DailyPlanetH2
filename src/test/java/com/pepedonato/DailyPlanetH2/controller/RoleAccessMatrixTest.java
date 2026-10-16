package com.pepedonato.DailyPlanetH2.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import com.pepedonato.DailyPlanetH2.repository.CommentoRepository;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Matrice rotte × ruoli: verifica le regole di accesso definite in
 * {@code SpringSecurityConfig} per utente anonimo, {@code ROLE_USER} e {@code ROLE_ADMIN}.
 *
 * <p>Le rotte pubbliche rispondono 200, le rotte protette senza autenticazione portano al
 * login (302) e un ruolo insufficiente viene rediretto alla pagina 403 (302) dal
 * {@code MyAccessDeniedHandler}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleAccessMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentoRepository commentoRepository;

    @Autowired
    private DomandaLavoroRepository domandaLavoroRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void preparaUtenti() {
        commentoRepository.deleteAll();
        domandaLavoroRepository.deleteAll();
        categoriaRepository.deleteAll();
        userRepository.deleteAll();

        for (String[] utente : new String[][]{{"utente.matricola", "USER"}, {"admin.matricola", "ADMIN"}}) {
            User u = new User();
            u.setUsername(utente[0]);
            u.setPassword(passwordEncoder.encode("password-di-prova-1"));
            u.setFirstname("Nome");
            u.setLastname("Cognome");
            u.setEmail(utente[0] + "@example.invalid");
            u.setUserrole(utente[1]);
            u.setDisabled(Boolean.FALSE);
            u.setMustChangePassword(Boolean.FALSE);
            userRepository.save(u);
        }
    }

    private static final String[] ROTTE_PUBBLICHE = {"/", "/about", "/home", "/login", "/403", "/actuator/health"};
    private static final String[] ROTTE_UTENTE = {"/user", "/user/edituser", "/user/newdomanda", "/user/changepw", "/user/listdomande"};
    private static final String[] ROTTE_ADMIN = {"/admin", "/admin/account", "/admin/newaccount"};

    @Test
    @DisplayName("Anonimo: rotte pubbliche 200, rotte protette → login")
    void anonimo() throws Exception {
        for (String rotta : ROTTE_PUBBLICHE) {
            mockMvc.perform(get(rotta)).andExpect(status().isOk());
        }
        for (String rotta : ROTTE_UTENTE) {
            mockMvc.perform(get(rotta)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
        for (String rotta : ROTTE_ADMIN) {
            mockMvc.perform(get(rotta)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    @WithMockUser(username = "utente.matricola", authorities = "ROLE_USER")
    @DisplayName("ROLE_USER: rotte utente 200, rotte admin → 403")
    void ruoloUtente() throws Exception {
        for (String rotta : ROTTE_PUBBLICHE) {
            mockMvc.perform(get(rotta)).andExpect(status().isOk());
        }
        for (String rotta : ROTTE_UTENTE) {
            mockMvc.perform(get(rotta)).andExpect(status().isOk());
        }
        for (String rotta : ROTTE_ADMIN) {
            mockMvc.perform(get(rotta)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/403"));
        }
    }

    @Test
    @WithMockUser(username = "admin.matricola", authorities = "ROLE_ADMIN")
    @DisplayName("ROLE_ADMIN: rotte utente e admin entrambe 200")
    void ruoloAdmin() throws Exception {
        for (String rotta : ROTTE_PUBBLICHE) {
            mockMvc.perform(get(rotta)).andExpect(status().isOk());
        }
        for (String rotta : ROTTE_UTENTE) {
            mockMvc.perform(get(rotta)).andExpect(status().isOk());
        }
        for (String rotta : ROTTE_ADMIN) {
            mockMvc.perform(get(rotta)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(username = "utente.matricola", authorities = "ROLE_USER")
    @DisplayName("Rotte inesistenti: 404 con pagina di errore, non whitelabel")
    void rottaInesistente() throws Exception {
        mockMvc.perform(get("/rotta-che-non-esiste")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Rotte inesistenti senza autenticazione: il controllo di accesso precede il 404")
    void rottaInesistenteAnonimo() throws Exception {
        mockMvc.perform(get("/rotta-che-non-esiste")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
