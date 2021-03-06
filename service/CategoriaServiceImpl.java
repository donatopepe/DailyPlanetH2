/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.service;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.repository.CategoriaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CategoriaServiceImpl {

    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private DomandaServiceImpl domandaServiceImpl;

    public Categoria findByNome(String nome) {
        return categoriaRepository.findByNomeIgnoreCase(nome);
    }

    public List<Categoria> findByDomanda(Long id) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id);
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
        return new Sort(Sort.Direction.ASC, "nome");
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
    public Categoria saveCategoria(Categoria categoria, DomandaLavoro domanda) {
        Categoria findByNome = findByNome(categoria.getNome());
        Categoria save;
        if (findByNome == null) {
            System.out.println("Categoria nuova aggiungo domanda e salvo in tabella");
            categoria.addDomanda(domanda);
            save = categoriaRepository.save(categoria);

        } else {
            System.out.println("Categoria già presente quindi aggiungo domanda attuale " + categoria.getNome());
            findByNome.addDomanda(domanda);
            save = categoriaRepository.save(findByNome);
        }
        if (save == null) {

            System.out.println("Salvataggio non riuscito in tabella categoria: " + categoria.getNome());
        } else {
            System.out.println("Salvata in tabella categoria: " + categoria.getNome());
        }
        return save;

    }

    public List<Categoria> saveCategorie(List<Categoria> categorie, DomandaLavoro domanda) {
        Boolean success = true;

        for (Categoria categoria2:categoriaRepository.findByDomandeOrderByNomeAsc(domanda)){
            System.out.println("Rimuovo categoria " + categoria2.getNome());
            categoria2.removeDomanda(domanda);
            
            if (categoriaRepository.save(categoria2)!=null) {
                System.out.println("ok aggiornamento categoria");
            }
        }

        /*devo prima rimuovere tutte le categorie associate alla domanda */
        categorie.forEach((categoria) -> {
            categorie.set(categorie.indexOf(categoria), saveCategoria(categoria, domanda));
        });

        return categorie;
    }

}
