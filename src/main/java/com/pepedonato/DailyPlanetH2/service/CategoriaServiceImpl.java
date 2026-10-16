/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.service;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CategoriaServiceImpl {

    @Autowired
    private CategoriaRepository categoriaRepository;
    // Repository usato al posto di DomandaServiceImpl: evita la dipendenza circolare
    // CategoriaServiceImpl <-> DomandaServiceImpl (errore di avvio del contesto).
    @Autowired
    private DomandaLavoroRepository domandaLavoroRepository;

    public Categoria findByNome(String nome) {
        return categoriaRepository.findByNomeIgnoreCase(nome);
    }

    public List<Categoria> findByDomanda(Long id) {
        DomandaLavoro loadDomandaLavoroById = domandaLavoroRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Domanda non trovata: " + id));
        return categoriaRepository.findByDomandeOrderByNomeAsc(loadDomandaLavoroById);
    }

    public Boolean NomeExist(String nome) {
        return findByNome(nome) != null;
    }

    public List<Categoria> getAll() {
        return categoriaRepository.findAll(sortBy());
        // return categoriaRepository.findAll();
    }

    private Sort sortBy() {
        return Sort.by(Sort.Direction.ASC, "nome");
    }

    /*
    public Categoria addCategoria(String nome) {
        Categoria findByNome;
        findByNome = findByNome(nome);
        if (findByNome != null) {
            return findByNome;
        } else {
            return new Categoria(nome);
        }
    }
     */
    /**
     * Ritrova o crea la categoria per nome. Se {@code domanda} è indicata aggiorna il lato
     * proprietario dell'associazione ({@code DomandaLavoro.categorie}), che è l'unico
     * rilevante per la persistenza.
     */
    public Categoria saveCategoria(Categoria categoria, DomandaLavoro domanda) {
        Categoria esistente = findByNome(categoria.getNome());
        Categoria salvata = categoriaRepository.save(esistente != null ? esistente : categoria);

        if (domanda != null && salvata != null) {
            if (domanda.getCategorie() == null) {
                domanda.setCategorie(new ArrayList<>());
            }
            boolean presente = domanda.getCategorie().stream()
                    .anyMatch(c -> c != null && c.getNome() != null
                            && c.getNome().equalsIgnoreCase(salvata.getNome()));
            if (!presente) {
                domanda.getCategorie().add(salvata);
            }
        }
        return salvata;
    }

    /**
     * Persiste (o ritrova) le categorie indicate e ritorna una lista NUOVA.
     *
     * <p>La lista di ingresso non viene modificata: la versione precedente usava
     * {@code List.set(...)} sulla lista del chiamante, causando
     * {@code UnsupportedOperationException} con liste immutabili.</p>
     */
    public List<Categoria> saveCategorie(List<Categoria> categorie, DomandaLavoro domanda) {
        List<Categoria> risultato = new ArrayList<>();
        if (categorie == null) {
            return risultato;
        }
        for (Categoria categoria : categorie) {
            if (categoria == null || categoria.getNome() == null || categoria.getNome().trim().isEmpty()) {
                continue;
            }
            risultato.add(saveCategoria(categoria, null));
        }
        return risultato;
    }

}
