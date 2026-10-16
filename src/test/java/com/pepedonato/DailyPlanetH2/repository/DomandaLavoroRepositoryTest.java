package com.pepedonato.DailyPlanetH2.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Test di persistenza delle domande di lavoro: query derivate, ordinamenti e caricamento
 * anticipato di {@code categorie}/{@code user} (necessario perché l'applicazione usa
 * {@code open-in-view: false}).
 */
@DataJpaTest
class DomandaLavoroRepositoryTest {

    @Autowired
    private DomandaLavoroRepository domandaLavoroRepository;

    @Autowired
    private CommentoRepository commentoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EntityManager entityManager;

    private User utente;
    private Categoria categoria;
    private DomandaLavoro domanda;

    @BeforeEach
    void prepara() {
        utente = new User();
        utente.setUsername("utente.repo");
        utente.setPassword("$2a$10$hashfintoperiltest");
        utente.setFirstname("Nome");
        utente.setLastname("Cognome");
        utente.setEmail("utente.repo@example.invalid");
        utente.setUserrole("USER");
        utente.setDisabled(Boolean.FALSE);
        utente = userRepository.save(utente);

        categoria = new Categoria();
        categoria.setNome("Manutenzione");
        categoria = categoriaRepository.save(categoria);

        domanda = new DomandaLavoro();
        domanda.setUser(utente);
        domanda.setTitolo("Sostituzione filtro");
        domanda.setDescrizione("Descrizione della domanda");
        domanda.setClosed(Boolean.FALSE);
        domanda.setDatacreazione(Timestamp.from(Instant.now()));
        domanda.setCategorie(List.of(categoria));
        domanda = domandaLavoroRepository.save(domanda);
    }

    @Test
    @DisplayName("findByUserOrderByDatamodificaDesc ritorna le domande dell'utente con categorie caricate")
    void findByUserConCategorieCaricate() {
        List<DomandaLavoro> domande = domandaLavoroRepository.findByUserOrderByDatamodificaDesc(utente);
        assertEquals(1, domande.size());

        // simula la fine della sessione Hibernate (come a fine richiesta HTTP)
        entityManager.flush();
        entityManager.clear();

        DomandaLavoro ricaricata = domande.get(0);
        assertEquals("Sostituzione filtro", ricaricata.getTitolo());
        assertEquals(1, ricaricata.getCategorie().size());
        assertEquals("Manutenzione", ricaricata.getCategorie().get(0).getNome());
        assertEquals("utente.repo", ricaricata.getUser().getUsername());
    }

    @Test
    @DisplayName("findAll carica categorie e autore (usato dall'elenco domande)")
    void findAllConEntityGraph() {
        entityManager.flush();
        entityManager.clear();

        List<DomandaLavoro> tutte = domandaLavoroRepository.findAll();
        assertEquals(1, tutte.size());
        assertEquals(1, tutte.get(0).getCategorie().size());
        assertNotNull(tutte.get(0).getUser());
    }

    @Test
    @DisplayName("findByCategorieOrderByDatamodificaDesc filtra per categoria")
    void findByCategorie() {
        List<DomandaLavoro> trovate = domandaLavoroRepository
                .findByCategorieOrderByDatamodificaDesc(List.of(categoria));

        assertEquals(1, trovate.size());
        assertFalse(trovate.get(0).getClosed());
    }

    @Test
    @DisplayName("Una domanda può avere PIÙ commenti (regressione: prima era @OneToOne)")
    void piuCommentiPerDomanda() {
        Commento primo = new Commento();
        primo.setUser(utente);
        primo.setDomanda(domanda);
        primo.setDescrizione("Primo commento");
        primo.setRisposta(Boolean.FALSE);
        primo.setDatacreazione(Timestamp.from(Instant.now()));
        commentoRepository.save(primo);

        Commento secondo = new Commento();
        secondo.setUser(utente);
        secondo.setDomanda(domanda);
        secondo.setDescrizione("Secondo commento");
        secondo.setRisposta(Boolean.FALSE);
        secondo.setDatacreazione(Timestamp.from(Instant.now()));
        commentoRepository.save(secondo);

        List<Commento> commenti = commentoRepository.findByDomandaOrderByDatamodificaAsc(domanda);
        assertEquals(2, commenti.size(), "devono essere possibili più commenti sulla stessa domanda");
        assertTrue(commenti.stream().allMatch(c -> c.getUser() != null));
    }

    @Test
    @DisplayName("findByDomandaAndRispostaOrderByDatamodificaAsc separa commenti e risposte")
    void separaCommentiERisposte() {
        Commento commento = new Commento();
        commento.setUser(utente);
        commento.setDomanda(domanda);
        commento.setDescrizione("Commento semplice");
        commento.setRisposta(Boolean.FALSE);
        commento.setDatacreazione(Timestamp.from(Instant.now()));
        commento = commentoRepository.save(commento);

        Commento risposta = new Commento();
        risposta.setUser(utente);
        risposta.setDomanda(domanda);
        risposta.setCommento(commento);
        risposta.setDescrizione("Risposta al commento");
        risposta.setRisposta(Boolean.TRUE);
        risposta.setDatacreazione(Timestamp.from(Instant.now()));
        commentoRepository.save(risposta);

        assertEquals(1, commentoRepository.findByDomandaAndRispostaOrderByDatamodificaAsc(domanda, Boolean.FALSE).size());
        assertEquals(1, commentoRepository.findByDomandaAndRispostaOrderByDatamodificaAsc(domanda, Boolean.TRUE).size());
    }
}
