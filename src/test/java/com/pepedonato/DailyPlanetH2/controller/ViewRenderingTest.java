package com.pepedonato.DailyPlanetH2.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import com.pepedonato.DailyPlanetH2.repository.CommentoRepository;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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

/**
 * Verifica che tutte le viste Thymeleaf referenziate dai controller esistano e vengano
 * renderizzate (MockMvc esegue il rendering reale: un template assente o con errori di
 * sintassi fa fallire il test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViewRenderingTest {

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

    private Long domandaId;
    private Long commentoId;
    private Long adminId;
    private Long userId;
    private String categoriaNome;

    @BeforeEach
    void preparaDati() {
        commentoRepository.deleteAll();
        domandaLavoroRepository.deleteAll();
        categoriaRepository.deleteAll();
        userRepository.deleteAll();

        User admin = user("admin.test", "ADMIN");
        User utente = user("user.test", "USER");
        adminId = admin.getId();
        userId = utente.getId();

        Categoria categoria = new Categoria();
        categoria.setNome("Manutenzione");
        categoria = categoriaRepository.save(categoria);
        categoriaNome = categoria.getNome();

        DomandaLavoro domanda = new DomandaLavoro();
        domanda.setUser(utente);
        domanda.setTitolo("Sostituzione filtro");
        domanda.setDescrizione("Il filtro dell'aria va sostituito.");
        domanda.setClosed(Boolean.FALSE);
        domanda.setDatacreazione(Timestamp.from(Instant.now()));
        List<Categoria> categorie = new ArrayList<>();
        categorie.add(categoria);
        domanda.setCategorie(categorie);
        domanda = domandaLavoroRepository.save(domanda);
        domandaId = domanda.getId();

        Commento commento = new Commento();
        commento.setUser(utente);
        commento.setDomanda(domanda);
        commento.setDescrizione("Serve un tecnico.");
        commento.setRisposta(Boolean.FALSE);
        commento = commentoRepository.save(commento);
        commentoId = commento.getId();

        Commento risposta = new Commento();
        risposta.setUser(admin);
        risposta.setDomanda(domanda);
        risposta.setCommento(commento);
        risposta.setDescrizione("Tecnico assegnato.");
        risposta.setRisposta(Boolean.TRUE);
        commentoRepository.save(risposta);
    }

    private User user(String username, String ruolo) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password-di-prova-1"));
        user.setFirstname("Nome");
        user.setLastname("Cognome");
        user.setEmail(username + "@example.invalid");
        user.setUserrole(ruolo);
        user.setDisabled(Boolean.FALSE);
        user.setMustChangePassword(Boolean.FALSE);
        return userRepository.save(user);
    }

    // --- pagine pubbliche ----------------------------------------------------------------

    @Test
    @DisplayName("Pagine pubbliche: /, /about, /login e 403")
    void paginePubbliche() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("about"));
        mockMvc.perform(get("/about")).andExpect(status().isOk()).andExpect(view().name("about"));
        mockMvc.perform(get("/home")).andExpect(status().isOk()).andExpect(view().name("about"));
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
        mockMvc.perform(get("/403")).andExpect(status().isOk()).andExpect(view().name("error/403"));
    }

    @Test
    @DisplayName("Anonimo: area utente e amministratore portano al login")
    void anonimoRedirettoAlLogin() throws Exception {
        mockMvc.perform(get("/user")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- area utente ---------------------------------------------------------------------

    @Test
    @WithMockUser(username = "user.test", authorities = "ROLE_USER")
    @DisplayName("Area utente: tutte le viste GET renderizzate")
    void areaUtente() throws Exception {
        mockMvc.perform(get("/user")).andExpect(status().isOk()).andExpect(view().name("user"));
        mockMvc.perform(get("/user/edituser")).andExpect(status().isOk()).andExpect(view().name("edituser"));
        mockMvc.perform(get("/user/newdomanda")).andExpect(status().isOk()).andExpect(view().name("newdomanda"));
        mockMvc.perform(get("/user/changepw")).andExpect(status().isOk()).andExpect(view().name("changepw"));

        mockMvc.perform(get("/user/domanda/" + domandaId)).andExpect(status().isOk()).andExpect(view().name("domanda"));
        mockMvc.perform(get("/user/editcommento/" + domandaId + "/" + commentoId))
                .andExpect(status().isOk()).andExpect(view().name("domanda"));
        mockMvc.perform(get("/user/editrisposta/" + domandaId + "/" + commentoId))
                .andExpect(status().isOk()).andExpect(view().name("domanda"));
    }

    @Test
    @WithMockUser(username = "user.test", authorities = "ROLE_USER")
    @DisplayName("Frammenti utente: elenco domande e commenti")
    void frammentiUtente() throws Exception {
        mockMvc.perform(get("/user/listdomande")).andExpect(status().isOk())
                .andExpect(view().name("listdomande :: resultsList"));
        mockMvc.perform(get("/user/listdomande/user/user.test")).andExpect(status().isOk())
                .andExpect(view().name("listdomande :: resultsList"));
        mockMvc.perform(get("/user/listdomande/category/" + categoriaNome)).andExpect(status().isOk())
                .andExpect(view().name("listdomande :: resultsList"));
        // La rotta riceve l'id di un COMMENTO (vedi nota in TODO.md, S5: nome parametro da chiarire)
        mockMvc.perform(get("/user/listcommenti/" + commentoId)).andExpect(status().isOk())
                .andExpect(view().name("listcommentDo :: results"));
        // id inesistente: nessuna eccezione, elenco vuoto
        mockMvc.perform(get("/user/listcommenti/999999")).andExpect(status().isOk())
                .andExpect(view().name("listcommentDo :: results"));
    }

    @Test
    @WithMockUser(username = "user.test", authorities = "ROLE_USER")
    @DisplayName("Ruolo USER non accede all'area amministratore: redirect a /403")
    void utenteNonAccedeAdAdmin() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "user.test", authorities = "ROLE_USER")
    @DisplayName("Domanda inesistente: redirect all'area utente")
    void domandaInesistente() throws Exception {
        mockMvc.perform(get("/user/domanda/999999")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    // --- area amministratore -------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin.test", authorities = "ROLE_ADMIN")
    @DisplayName("Area amministratore: tutte le viste GET renderizzate")
    void areaAmministratore() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isOk()).andExpect(view().name("admin"));
        mockMvc.perform(get("/admin/account")).andExpect(status().isOk()).andExpect(view().name("account"));
        mockMvc.perform(get("/admin/newaccount")).andExpect(status().isOk()).andExpect(view().name("newaccount"));
        mockMvc.perform(get("/admin/edit/" + userId)).andExpect(status().isOk()).andExpect(view().name("editaccount"));
        mockMvc.perform(get("/admin/changepw/" + userId)).andExpect(status().isOk())
                .andExpect(view().name("adminchangepw"));
    }

    @Test
    @WithMockUser(username = "admin.test", authorities = "ROLE_ADMIN")
    @DisplayName("Form inviato senza token CSRF: rifiutato (CSRF attivo)")
    void csrfAttivo() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/doSaveAccount")
                        .param("username", "nuovo.utente")
                        .param("password", "password-di-prova-2")
                        .param("firstname", "Nuovo")
                        .param("lastname", "Utente")
                        .param("email", "nuovo@example.invalid")
                        .param("userrole", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "admin.test", authorities = "ROLE_ADMIN")
    @DisplayName("Form inviato con token CSRF: accettato")
    void csrfConTokenAccettato() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/doSaveAccount").with(csrf())
                        .param("username", "nuovo.utente")
                        .param("password", "password-di-prova-2")
                        .param("firstname", "Nuovo")
                        .param("lastname", "Utente")
                        .param("email", "nuovo@example.invalid")
                        .param("userrole", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("newaccountDo :: results"));
    }

    @Test
    @WithMockUser(username = "admin.test", authorities = "ROLE_ADMIN")
    @DisplayName("Amministratore: cambio password obbligatorio reindirizza a /user/changepw")
    void cambioPasswordObbligatorio() throws Exception {
        User admin = userRepository.findByUsername("admin.test");
        admin.setMustChangePassword(Boolean.TRUE);
        userRepository.save(admin);

        mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/changepw"));

        // la pagina di cambio password resta raggiungibile
        mockMvc.perform(get("/user/changepw")).andExpect(status().isOk()).andExpect(view().name("changepw"));
    }

    @Test
    @DisplayName("Admin id inutilizzato: verifica consistenza dati di test")
    void datiDiTestCoerenti() {
        org.junit.jupiter.api.Assertions.assertNotNull(adminId);
        org.junit.jupiter.api.Assertions.assertNotNull(userId);
    }
}
