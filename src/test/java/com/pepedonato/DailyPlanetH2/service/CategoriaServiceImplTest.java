package com.pepedonato.DailyPlanetH2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/**
 * Test unitari di {@link CategoriaServiceImpl} con repository simulati.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private DomandaLavoroRepository domandaLavoroRepository;

    @InjectMocks
    private CategoriaServiceImpl service;

    private Categoria categoria(String nome) {
        Categoria c = new Categoria();
        c.setNome(nome);
        return c;
    }

    private DomandaLavoro domanda() {
        DomandaLavoro d = new DomandaLavoro();
        d.setTitolo("Titolo");
        d.setDescrizione("Descrizione lunga");
        return d;
    }

    @Test
    @DisplayName("findByNome delega al repository; NomeExist riflette il risultato")
    void findByNomeENomeExist() {
        when(categoriaRepository.findByNomeIgnoreCase("Manutenzione")).thenReturn(categoria("Manutenzione"));
        when(categoriaRepository.findByNomeIgnoreCase("Assente")).thenReturn(null);

        assertEquals("Manutenzione", service.findByNome("Manutenzione").getNome());
        assertTrue(service.NomeExist("Manutenzione"));
        assertNull(service.findByNome("Assente"));
        assertFalse(service.NomeExist("Assente"));
    }

    @Test
    @DisplayName("findByDomanda usa DomandaLavoroRepository e propaga l'assenza della domanda")
    void findByDomanda() {
        DomandaLavoro domanda = domanda();
        when(domandaLavoroRepository.findById(10L)).thenReturn(Optional.of(domanda));
        when(categoriaRepository.findByDomandeOrderByNomeAsc(domanda)).thenReturn(List.of(categoria("Manutenzione")));

        List<Categoria> trovate = service.findByDomanda(10L);
        assertEquals(1, trovate.size());
        assertEquals("Manutenzione", trovate.get(0).getNome());

        when(domandaLavoroRepository.findById(11L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.findByDomanda(11L));
    }

    @Test
    @DisplayName("saveCategoria: categoria esistente riusata, categoria nuova salvata")
    void saveCategoriaNuovaOEsistente() {
        Categoria esistente = categoria("Manutenzione");
        when(categoriaRepository.findByNomeIgnoreCase("Manutenzione")).thenReturn(esistente);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        service.saveCategoria(categoria("Manutenzione"), domanda());
        verify(categoriaRepository).save(esistente);

        Categoria nuova = categoria("Nuova");
        when(categoriaRepository.findByNomeIgnoreCase("Nuova")).thenReturn(null);
        Categoria salvata = service.saveCategoria(nuova, domanda());
        assertEquals("Nuova", salvata.getNome());
        verify(categoriaRepository).save(nuova);
    }

    @Test
    @DisplayName("getAll usa l'ordinamento per nome")
    void getAllUsaOrdinamentoPerNome() {
        when(categoriaRepository.findAll(any(Sort.class))).thenReturn(List.of(categoria("A"), categoria("B")));

        assertEquals(2, service.getAll().size());
        verify(categoriaRepository).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("saveCategorie senza domanda non cerca le categorie già associate")
    void saveCategorieSenzaDomanda() {
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        service.saveCategorie(List.of(categoria("Nuova")), null);

        verify(categoriaRepository, never()).findByDomandeOrderByNomeAsc(any());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("saveCategorie non modifica la lista di ingresso (regressione: era UnsupportedOperationException)")
    void saveCategorieNonModificaLaListaDiIngresso() {
        when(categoriaRepository.findByNomeIgnoreCase("Nuova")).thenReturn(null);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        List<Categoria> ingresso = List.of(categoria("Nuova"));
        List<Categoria> risultato = service.saveCategorie(ingresso, null);

        assertEquals(1, risultato.size());
        assertEquals(1, ingresso.size(), "la lista di ingresso non deve essere toccata");
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("saveCategoria con domanda aggiorna il lato proprietario dell'associazione")
    void saveCategoriaAggiornaLatoProprietario() {
        DomandaLavoro domanda = domanda();
        when(categoriaRepository.findByNomeIgnoreCase("Manutenzione")).thenReturn(null);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        Categoria salvata = service.saveCategoria(categoria("Manutenzione"), domanda);

        assertEquals("Manutenzione", salvata.getNome());
        assertEquals(1, domanda.getCategorie().size());
        assertEquals("Manutenzione", domanda.getCategorie().get(0).getNome());
    }
}
