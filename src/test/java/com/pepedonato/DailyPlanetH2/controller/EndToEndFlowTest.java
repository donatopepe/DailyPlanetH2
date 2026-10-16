package com.pepedonato.DailyPlanetH2.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import com.pepedonato.DailyPlanetH2.repository.CommentoRepository;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Flusso end-to-end su Tomcat simulato: login → creazione domanda → commento →
 * visualizzazione della domanda → logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndToEndFlowTest {

    private static final String USERNAME = "utente.e2e";
    private static final String PASSWORD = "password-di-prova-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private DomandaLavoroRepository domandaLavoroRepository;

    @Autowired
    private CommentoRepository commentoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void preparaUtente() {
        commentoRepository.deleteAll();
        domandaLavoroRepository.deleteAll();
        categoriaRepository.deleteAll();
        userRepository.deleteAll();

        User utente = new User();
        utente.setUsername(USERNAME);
        utente.setPassword(passwordEncoder.encode(PASSWORD));
        utente.setFirstname("Utente");
        utente.setLastname("EndToEnd");
        utente.setEmail("utente.e2e@example.invalid");
        utente.setUserrole("USER");
        utente.setDisabled(Boolean.FALSE);
        utente.setMustChangePassword(Boolean.FALSE);
        userRepository.save(utente);
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "il login deve creare una sessione autenticata");
        return session;
    }

    @Test
    @DisplayName("Flusso completo: login, nuova domanda, commento, lettura, logout")
    void flussoCompleto() throws Exception {
        MockHttpSession session = login();

        // area utente accessibile dopo il login
        mockMvc.perform(get("/user").session(session)).andExpect(status().isOk());

        // creazione domanda di lavoro
        mockMvc.perform(post("/user/doNewDomanda/SaveQuestion").session(session).with(csrf())
                        .param("titolo", "Verifica impianto")
                        .param("descrizione", "Controllare la pressione dell'impianto.")
                        .param("closed", "false")
                        .param("categorie", "Manutenzione"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        User utente = userRepository.findByUsername(USERNAME);
        List<DomandaLavoro> domande = domandaLavoroRepository.findByUserOrderByDatamodificaDesc(utente);
        assertEquals(1, domande.size(), "la domanda creata via form deve essere persistita");
        DomandaLavoro domanda = domande.get(0);
        assertEquals("Verifica impianto", domanda.getTitolo());
        assertFalse(Boolean.TRUE.equals(domanda.getClosed()));

        // commento alla domanda
        String testoCommento = "Aggiungere valvola di sicurezza.";
        mockMvc.perform(post("/user/doNewCommento/" + domanda.getId()).session(session).with(csrf())
                        .param("descrizione", testoCommento))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/domanda/" + domanda.getId() + "?alert=Commento+salvato"));

        List<Commento> commenti = commentoRepository.findByDomandaOrderByDatamodificaAsc(domanda);
        assertEquals(1, commenti.size(), "il commento deve essere persistito");
        assertEquals(testoCommento, commenti.get(0).getDescrizione());
        assertFalse(Boolean.TRUE.equals(commenti.get(0).getRisposta()));
        assertEquals(USERNAME, commenti.get(0).getUser().getUsername());

        // lettura della domanda: il commento è visibile nella pagina
        mockMvc.perform(get("/user/domanda/" + domanda.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(testoCommento)));

        // logout e verifica della protezione delle rotte
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/user").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Login con password errata: nessuna sessione autenticata")
    void loginConPasswordErrata() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", USERNAME)
                        .param("password", "password-sbagliata"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        assertTrue(domandaLavoroRepository.findAll().isEmpty());
    }
}
