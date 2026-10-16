package com.pepedonato.DailyPlanetH2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
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
 * Test unitari di {@link DomandaServiceImpl}: uso dell'utente autenticato, gestione
 * dell'assenza dell'entità, filtri per categoria.
 */
@ExtendWith(MockitoExtension.class)
class DomandaServiceImplTest {

    @Mock
    private DomandaLavoroRepository domandaLavoroRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoriaServiceImpl categoriaServiceImpl;

    @InjectMocks
    private DomandaServiceImpl service;

    @AfterEach
    void pulisciContesto() {
        SecurityContextHolder.clearContext();
    }

    private void autentica(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", List.of()));
    }

    private DomandaLavoro domanda() {
        DomandaLavoro d = new DomandaLavoro();
        d.setTitolo("Titolo");
        d.setDescrizione("Descrizione lunga");
        d.setCategorie(List.of());
        return d;
    }

    @Test
    @DisplayName("loadDomandaLavoroById ritorna null se la domanda non esiste")
    void loadDomandaInesistente() {
        when(domandaLavoroRepository.findById(99L)).thenReturn(Optional.empty());
        assertNull(service.loadDomandaLavoroById(99L));
    }

    @Test
    @DisplayName("findByCategoria normalizza il nome e delega al repository")
    void findByCategoria() {
        Categoria categoria = new Categoria();
        categoria.setNome("Manutenzione");
        when(categoriaServiceImpl.findByNome("Manutenzione")).thenReturn(categoria);
        when(domandaLavoroRepository.findByCategorieOrderByDatamodificaDesc(anyList()))
                .thenReturn(List.of(domanda()));

        assertEquals(1, service.findByCategoria("  Manutenzione  ").size());
        verify(categoriaServiceImpl).findByNome("Manutenzione");
    }

    @Test
    @DisplayName("saveDomandaLavoro imposta l'utente autenticato e salva")
    void saveDomandaImpostaUtente() {
        autentica("user.test");
        User user = new User();
        user.setUsername("user.test");
        when(userRepository.findByUsername("user.test")).thenReturn(user);
        when(categoriaServiceImpl.saveCategorie(anyList(), any())).thenReturn(List.of());
        when(domandaLavoroRepository.save(any(DomandaLavoro.class))).thenAnswer(i -> i.getArgument(0));

        DomandaLavoro nuova = domanda();
        assertTrue(service.saveDomandaLavoro(nuova));

        assertEquals("user.test", nuova.getUser().getUsername());
        assertTrue(nuova.getDatacreazione() != null);
    }

    @Test
    @DisplayName("findByUser usa l'utente indicato")
    void findByUser() {
        User user = new User();
        user.setUsername("user.test");
        when(userRepository.findByUsername("user.test")).thenReturn(user);
        when(domandaLavoroRepository.findByUserOrderByDatamodificaDesc(user)).thenReturn(List.of(domanda()));

        assertEquals(1, service.findByUser("user.test").size());
    }

    @Test
    @DisplayName("getAll usa l'ordinamento per data di modifica decrescente")
    void getAll() {
        when(domandaLavoroRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(domanda()));

        assertFalse(service.getAll().isEmpty());
    }
}
