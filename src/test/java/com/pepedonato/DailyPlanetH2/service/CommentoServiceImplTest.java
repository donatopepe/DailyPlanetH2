package com.pepedonato.DailyPlanetH2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.CommentoRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test unitari di {@link CommentoServiceImpl}: robustezza su id inesistenti e
 * associazione dell'utente autenticato.
 */
@ExtendWith(MockitoExtension.class)
class CommentoServiceImplTest {

    @Mock
    private CommentoRepository commentoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomandaServiceImpl domandaServiceImpl;

    @InjectMocks
    private CommentoServiceImpl service;

    @AfterEach
    void pulisciContesto() {
        SecurityContextHolder.clearContext();
    }

    private Commento commento(String testo) {
        Commento c = new Commento();
        c.setDescrizione(testo);
        c.setRisposta(Boolean.FALSE);
        return c;
    }

    @Test
    @DisplayName("loadCommentoById ritorna null se il commento non esiste (nessuna eccezione)")
    void loadCommentoInesistente() {
        when(commentoRepository.findById(99L)).thenReturn(Optional.empty());
        assertNull(service.loadCommentoById(99L));
    }

    @Test
    @DisplayName("findByCommento ritorna elenco vuoto se il commento genitore non esiste")
    void findByCommentoGenitoreInesistente() {
        when(commentoRepository.findById(5L)).thenReturn(Optional.empty());
        assertTrue(service.findByCommento(5L).isEmpty());
    }

    @Test
    @DisplayName("findByCommento delega al repository quando il genitore esiste")
    void findByCommentoConGenitore() {
        Commento genitore = commento("Commento genitore");
        when(commentoRepository.findById(5L)).thenReturn(Optional.of(genitore));
        when(commentoRepository.findByCommentoOrderByDatamodificaAsc(genitore))
                .thenReturn(List.of(commento("Risposta al commento")));

        assertEquals(1, service.findByCommento(5L).size());
    }

    @Test
    @DisplayName("saveCommentoDomanda associa utente e domanda e imposta la data di creazione")
    void saveCommentoDomanda() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user.test", "n/a", List.of()));
        User user = new User();
        user.setUsername("user.test");
        DomandaLavoro domanda = new DomandaLavoro();
        domanda.setTitolo("Titolo");
        when(userRepository.findByUsername("user.test")).thenReturn(user);
        when(domandaServiceImpl.loadDomandaLavoroById(3L)).thenReturn(domanda);
        when(commentoRepository.save(any(Commento.class))).thenAnswer(i -> i.getArgument(0));

        Commento nuovo = commento("Commento nuovo");
        assertTrue(service.saveCommentoDomanda(3L, nuovo));

        assertEquals("user.test", nuovo.getUser().getUsername());
        assertEquals(domanda, nuovo.getDomanda());
        assertTrue(nuovo.getDatacreazione() != null);
        assertFalse(Boolean.TRUE.equals(nuovo.getRisposta()));
    }

    @Test
    @DisplayName("findByDomandaRisposta filtra per domanda e tipo")
    void findByDomandaRisposta() {
        DomandaLavoro domanda = new DomandaLavoro();
        when(domandaServiceImpl.loadDomandaLavoroById(3L)).thenReturn(domanda);
        when(commentoRepository.findByDomandaAndRispostaOrderByDatamodificaAsc(domanda, Boolean.TRUE))
                .thenReturn(List.of(commento("Risposta")));

        assertEquals(1, service.findByDomandaRisposta(3L, Boolean.TRUE).size());
    }
}
