package com.pepedonato.DailyPlanetH2.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import com.pepedonato.DailyPlanetH2.repository.CommentoRepository;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * Verifica i flussi reali dell'interfaccia: la pagina viene letta, i campi del form vengono
 * estratti dall'HTML renderizzato e reinviati come farebbe un browser.
 *
 * <p>Questo test coglie la classe di difetti più insidiosa: un form il cui campo non
 * corrisponde a ciò che il controller legge (es. {@code categoria} nella pagina e
 * {@code categorie} nel controller). I test che inviano parametri scritti a mano non lo
 * rilevano, perché non dipendono dal template.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormFlowTest {

    private static final String UTENTE = "utente.form";
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

    private String nomeCategoria;

    @BeforeEach
    void preparaDati() {
        commentoRepository.deleteAll();
        domandaLavoroRepository.deleteAll();
        categoriaRepository.deleteAll();
        userRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNome("Manutenzione");
        nomeCategoria = categoriaRepository.save(categoria).getNome();

        userRepository.save(utente(UTENTE, "USER"));
        userRepository.save(utente("admin.form", "ADMIN"));
    }

    private User utente(String username, String ruolo) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFirstname("Nome");
        user.setLastname("Cognome");
        user.setEmail(username + "@example.invalid");
        user.setUserrole(ruolo);
        user.setDisabled(Boolean.FALSE);
        user.setMustChangePassword(Boolean.FALSE);
        return user;
    }

    // ------------------------------------------------------------------ strumenti di test

    private String pagina(String url) throws Exception {
        return mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Nomi dei campi del form che contiene l'azione indicata, escludendo i campi tecnici. */
    private Set<String> campiDelForm(String html, String azione) {
        int inizio = html.indexOf(azione);
        assertTrue(inizio > 0, "form non trovato nell'HTML per l'azione: " + azione);
        int fine = html.indexOf("</form>", inizio);
        String form = html.substring(inizio, fine);

        Set<String> campi = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("name=\"([^\"]+)\"").matcher(form);
        while (matcher.find()) {
            String nome = matcher.group(1);
            if (!nome.startsWith("_")) {   // _csrf e i campi ausiliari dei checkbox
                campi.add(nome);
            }
        }
        return campi;
    }

    /**
     * Invia il form usando esattamente i campi trovati nella pagina: se un campo non ha un
     * valore previsto dal test, il test fallisce con l'elenco, segnalando la divergenza.
     */
    private org.springframework.test.web.servlet.ResultActions inviaForm(String azione, Set<String> campi,
                                                                         Map<String, String> valori) throws Exception {
        for (String campo : campi) {
            assertTrue(valori.containsKey(campo),
                    "il form espone il campo '" + campo + "' ma il test non lo valorizza: campi=" + campi);
        }
        var richiesta = post(azione).with(csrf());
        for (String campo : campi) {
            richiesta = richiesta.param(campo, valori.get(campo));
        }
        return mockMvc.perform(richiesta);
    }

    // ------------------------------------------------------------------ flussi utente

    @Test
    @WithMockUser(username = UTENTE, authorities = "ROLE_USER")
    @DisplayName("Nuova domanda: il form renderizzato salva davvero domanda e categoria")
    void nuovaDomandaDalForm() throws Exception {
        String html = pagina("/user/newdomanda");
        Set<String> campi = campiDelForm(html, "/user/doNewDomanda/SaveQuestion");
        assertTrue(campi.containsAll(List.of("titolo", "descrizione", "categoria")),
                "campi attesi nella pagina, trovati: " + campi);

        Map<String, String> valori = new LinkedHashMap<>();
        valori.put("id_domanda", "");
        valori.put("titolo", "Domanda creata dal form");
        valori.put("descrizione", "Descrizione valida scritta nel form dell'interfaccia.");
        valori.put("categoria", nomeCategoria);
        valori.put("closed", "false");

        inviaForm("/user/doNewDomanda/SaveQuestion", campi, valori)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        User autore = userRepository.findByUsername(UTENTE);
        List<DomandaLavoro> domande = domandaLavoroRepository.findByUserOrderByDatamodificaDesc(autore);
        assertEquals(1, domande.size(), "la domanda deve essere salvata dal form dell'interfaccia");
        DomandaLavoro domanda = domande.get(0);
        assertEquals("Domanda creata dal form", domanda.getTitolo());
        assertFalse(domanda.getCategorie().isEmpty(), "la categoria scelta nel form deve essere associata");
        assertEquals(nomeCategoria, domanda.getCategorie().get(0).getNome());

        // la domanda compare nell'elenco (pagina completa, non un frammento)
        String elenco = pagina("/user/listdomande?categoria=" + nomeCategoria);
        assertTrue(elenco.contains("Domanda creata dal form"), "la domanda deve comparire nell'elenco filtrato");
        assertTrue(elenco.contains("<nav>"), "l'elenco deve essere una pagina completa con navigazione");
    }

    @Test
    @WithMockUser(username = UTENTE, authorities = "ROLE_USER")
    @DisplayName("Commento e risposta: i form della pagina domanda salvano e riportano alla domanda")
    void commentoERispostaDalForm() throws Exception {
        DomandaLavoro domanda = salvaDomanda();

        String paginaDomanda = pagina("/user/domanda/" + domanda.getId());
        Set<String> campiCommento = campiDelForm(paginaDomanda, "/user/doNewCommento/" + domanda.getId());
        assertEquals(Set.of("descrizione"), campiCommento);

        inviaForm("/user/doNewCommento/" + domanda.getId(), campiCommento,
                Map.of("descrizione", "Commento scritto dal form dell'interfaccia."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/domanda/" + domanda.getId() + "?alert=Commento+salvato"));

        Set<String> campiRisposta = campiDelForm(paginaDomanda, "/user/doNewAnswer/" + domanda.getId());
        assertEquals(Set.of("descrizione"), campiRisposta);
        inviaForm("/user/doNewAnswer/" + domanda.getId(), campiRisposta,
                Map.of("descrizione", "Risposta scritta dal form dell'interfaccia."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/domanda/" + domanda.getId() + "?alert=Risposta+salvata"));

        List<Commento> commenti = commentoRepository.findByDomandaOrderByDatamodificaAsc(domanda);
        assertEquals(2, commenti.size(), "commento e risposta devono essere salvati");
        assertTrue(commenti.stream().anyMatch(c -> Boolean.FALSE.equals(c.getRisposta())));
        assertTrue(commenti.stream().anyMatch(c -> Boolean.TRUE.equals(c.getRisposta())));

        // testo troppo corto: nessun salvataggio e messaggio di errore nella pagina
        String htmlErrore = mockMvc.perform(post("/user/doNewCommento/" + domanda.getId()).with(csrf())
                        .param("descrizione", "corto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/domanda/" + domanda.getId()
                        + "?errore=Il+commento+deve+avere+almeno+8+caratteri"))
                .andReturn().getResponse().getRedirectedUrl();
        assertNotNull(htmlErrore);
        assertEquals(2, commentoRepository.findByDomandaOrderByDatamodificaAsc(domanda).size());
    }

    @Test
    @WithMockUser(username = UTENTE, authorities = "ROLE_USER")
    @DisplayName("Modifica del proprio account dal form e cambio password")
    void modificaAccountECambioPasswordDalForm() throws Exception {
        String html = pagina("/user/edituser");
        Set<String> campi = campiDelForm(html, "/user/doEditUser");
        assertTrue(campi.containsAll(List.of("username", "firstname", "lastname", "email")),
                "campi attesi: " + campi);

        Map<String, String> valori = new LinkedHashMap<>();
        valori.put("id", String.valueOf(userRepository.findByUsername(UTENTE).getId()));
        valori.put("username", UTENTE);
        valori.put("firstname", "Nome");
        valori.put("lastname", "Cognome Aggiornato");
        valori.put("email", "aggiornato@example.invalid");

        inviaForm("/user/doEditUser", campi, valori)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?alert=Account+aggiornato"));

        assertEquals("Cognome Aggiornato", userRepository.findByUsername(UTENTE).getLastname());

        String paginaPassword = pagina("/user/changepw");
        Set<String> campiPassword = campiDelForm(paginaPassword, "/user/doChangepw");
        assertTrue(campiPassword.containsAll(
                List.of("username", "password", "new_password", "confirm_password")), "campi: " + campiPassword);

        inviaForm("/user/doChangepw", campiPassword, Map.of(
                "username", UTENTE,
                "password", PASSWORD,
                "new_password", "nuova-password-1",
                "confirm_password", "nuova-password-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?alert=Password+aggiornata"));

        User aggiornato = userRepository.findByUsername(UTENTE);
        assertTrue(passwordEncoder.matches("nuova-password-1", aggiornato.getPassword()));

        // password attuale errata: nessun salvataggio e messaggio di errore
        inviaForm("/user/doChangepw", campiPassword, Map.of(
                "username", UTENTE,
                "password", "password-sbagliata",
                "new_password", "altra-password-2",
                "confirm_password", "altra-password-2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/changepw?errore=Password+non+aggiornata+-"
                        + "+controllare+password+attuale+e+conferma"));
        assertTrue(passwordEncoder.matches("nuova-password-1", userRepository.findByUsername(UTENTE).getPassword()));
    }

    // ------------------------------------------------------------------ flussi amministratore

    @Test
    @WithMockUser(username = "admin.form", authorities = "ROLE_ADMIN")
    @DisplayName("Creazione account dal form, elenco e reimpostazione password")
    void gestioneAccountDalForm() throws Exception {
        String html = pagina("/admin/newaccount");
        Set<String> campi = campiDelForm(html, "/admin/doSaveAccount");
        assertTrue(campi.containsAll(List.of("username", "password", "firstname", "lastname", "email", "userrole")),
                "campi attesi: " + campi);

        Map<String, String> valori = new LinkedHashMap<>();
        valori.put("username", "nuovo.account");
        valori.put("password", "password-nuovo-1");
        valori.put("firstname", "Nuovo");
        valori.put("lastname", "Account");
        valori.put("email", "nuovo.account@example.invalid");
        valori.put("userrole", "USER");
        valori.put("disabled", "false");

        inviaForm("/admin/doSaveAccount", campi, valori)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/account?alert=Account+creato"));

        User creato = userRepository.findByUsername("nuovo.account");
        assertNotNull(creato, "l'account creato dal form deve esistere");
        assertTrue(creato.getPassword().startsWith("$2"), "la password deve essere un hash BCrypt");
        assertTrue(passwordEncoder.matches("password-nuovo-1", creato.getPassword()));
        assertTrue(Boolean.TRUE.equals(creato.getMustChangePassword()), "il nuovo account deve cambiare password");

        String elenco = pagina("/admin/account");
        assertTrue(elenco.contains("nuovo.account"), "l'elenco account deve mostrare l'account creato");

        String paginaPassword = pagina("/admin/changepw/" + creato.getId());
        Set<String> campiPassword = campiDelForm(paginaPassword, "/admin/doAdminChangepw");
        assertEquals(Set.of("id", "new_password", "confirm_password"), campiPassword);

        inviaForm("/admin/doAdminChangepw", campiPassword, Map.of(
                "id", String.valueOf(creato.getId()),
                "new_password", "password-reimpostata-1",
                "confirm_password", "password-reimpostata-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/account?alert=Password+reimpostata"));

        User reimpostato = userRepository.findByUsername("nuovo.account");
        assertTrue(passwordEncoder.matches("password-reimpostata-1", reimpostato.getPassword()));

        // nome utente duplicato: errore mostrato nel form, nessun duplicato creato
        inviaForm("/admin/doSaveAccount", campi, valori)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/newaccount?errore=Nome+utente+già+presente"));
    }

    private DomandaLavoro salvaDomanda() {
        DomandaLavoro domanda = new DomandaLavoro();
        domanda.setUser(userRepository.findByUsername(UTENTE));
        domanda.setTitolo("Domanda di prova");
        domanda.setDescrizione("Descrizione della domanda di prova.");
        domanda.setClosed(Boolean.FALSE);
        domanda.setDatacreazioneNow();
        return domandaLavoroRepository.save(domanda);
    }
}
