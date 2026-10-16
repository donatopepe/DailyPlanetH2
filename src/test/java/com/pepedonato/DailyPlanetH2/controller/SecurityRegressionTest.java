package com.pepedonato.DailyPlanetH2.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import com.pepedonato.DailyPlanetH2.repository.CommentoRepository;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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

/** Regression tests per IDOR, proprietà dei commenti, headers e session fixation. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityRegressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private DomandaLavoroRepository domandaRepository;
    @Autowired private CommentoRepository commentoRepository;
    @Autowired private PasswordEncoder encoder;

    private Long userId;
    private Long domandaId;
    private Long commentoId;

    @BeforeEach
    void prepara() {
        commentoRepository.deleteAll();
        domandaRepository.deleteAll();
        categoriaRepository.deleteAll();
        userRepository.deleteAll();
        User owner = saveUser("owner.security", "USER");
        userId = owner.getId();
        User victim = saveUser("victim.security", "USER");
        DomandaLavoro domanda = new DomandaLavoro();
        domanda.setUser(victim);
        domanda.setTitolo("Richiesta di verifica");
        domanda.setDescrizione("Descrizione valida per la richiesta di verifica.");
        domanda.setClosed(false);
        domanda.setDatacreazioneNow();
        domandaId = domandaRepository.save(domanda).getId();
        Commento commento = new Commento();
        commento.setUser(victim);
        commento.setDomanda(domanda);
        commento.setDescrizione("Commento scritto dal proprietario.");
        commento.setRisposta(false);
        commento.setDatacreazione(Timestamp.from(Instant.now()));
        commentoId = commentoRepository.save(commento).getId();
    }

    private User saveUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode("password-di-prova-1"));
        user.setFirstname("Nome");
        user.setLastname("Sicurezza");
        user.setEmail(username + "@example.invalid");
        user.setUserrole(role);
        user.setDisabled(false);
        user.setMustChangePassword(false);
        return userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "owner.security", authorities = "ROLE_USER")
    @DisplayName("Self-service non accetta ID altrui né permette di sovrascrivere lo username dal form")
    void idorProfiloRifiutato() throws Exception {
        mockMvc.perform(post("/user/doEditUser").with(csrf())
                        .param("id", String.valueOf(userId + 1))
                        .param("username", "victim.security")
                        .param("firstname", "Attaccante")
                        .param("lastname", "Modificato")
                        .param("email", "attaccante@example.invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/edituser?errore=Richiesta+non+valida"));

        org.junit.jupiter.api.Assertions.assertEquals("Nome",
                userRepository.findByUsername("victim.security").getFirstname());
    }

    @Test
    @WithMockUser(username = "owner.security", authorities = "ROLE_USER")
    @DisplayName("Self-service ignora ruoli e flag disabled inviati da client")
    void escalationRuoloRifiutata() throws Exception {
        mockMvc.perform(post("/user/doEditUser").with(csrf())
                        .param("id", String.valueOf(userId))
                        .param("username", "owner.security")
                        .param("firstname", "Nome")
                        .param("lastname", "Sicurezza")
                        .param("email", "owner.security@example.invalid")
                        .param("userrole", "ADMIN")
                        .param("disabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?alert=Account+aggiornato"));

        User reloaded = userRepository.findById(userId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("USER", reloaded.getUserrole());
        org.junit.jupiter.api.Assertions.assertFalse(reloaded.getDisabled());
    }

    @Test
    @WithMockUser(username = "owner.security", authorities = "ROLE_USER")
    @DisplayName("L'utente non può aprire il form di modifica di un commento altrui")
    void modificaCommentoAltruiNegata() throws Exception {
        mockMvc.perform(get("/user/editcommento/" + domandaId + "/" + commentoId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        mockMvc.perform(post("/user/editcommentocommento/" + domandaId + "/" + commentoId)
                        .with(csrf()).param("descrizione", "Testo alterato da utente non proprietario."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));

        org.junit.jupiter.api.Assertions.assertEquals("Commento scritto dal proprietario.",
                commentoRepository.findById(commentoId).orElseThrow().getDescrizione());
    }

    @Test
    @WithMockUser(username = "admin.security", authorities = "ROLE_ADMIN")
    @DisplayName("ROLE_ADMIN può aprire un commento altrui per moderazione")
    void adminPuoModerareCommento() throws Exception {
        mockMvc.perform(get("/user/editcommento/" + domandaId + "/" + commentoId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSP, nosniff, frame denial, referrer policy e cache-control sulle pagine private")
    @WithMockUser(username = "owner.security", authorities = "ROLE_USER")
    void headersDiSicurezza() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
