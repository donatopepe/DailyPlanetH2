/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.service;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.DomandaLavoroRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Service
public class DomandaServiceImpl {

    @Autowired
    private DomandaLavoroRepository domandaLavoroRepository;
    @Autowired
    private CategoriaServiceImpl categoriaServiceImpl;
    @Autowired
    private UserRepository userRepository;

    public List<DomandaLavoro> getAll() {
        return domandaLavoroRepository.findAll(sortBy());
        // return categoriaRepository.findAll();
    }

    private Sort sortBy() {
        return Sort.by(Sort.Direction.DESC, "datamodifica");
    }

    public DomandaLavoro loadDomandaLavoroById(Long id) {
        // Ritorna null se la domanda non esiste: i controller verificano il null
        // (prima Optional.get() causava NoSuchElementException -> errore 500).
        return domandaLavoroRepository.findById(id).orElse(null);
    }

    public List<DomandaLavoro> findByUser(String name) {
        User user = userRepository.findByUsername(name);
        return domandaLavoroRepository.findByUserOrderByDatamodificaDesc(user);
    }

    public List<DomandaLavoro> findByDates(Date start, Date end) {

        //return domandaLavoroRepository.findByDatamodificaBetween(new java.sql.Timestamp(start.getTime()), new java.sql.Timestamp(end.getTime()));
        
        return domandaLavoroRepository.findByDatamodificaAfterAndDatamodificaBeforeOrderByDatamodificaDesc(new java.sql.Timestamp(start.getTime()), new java.sql.Timestamp(end.getTime()));

    }

    public List<DomandaLavoro> findByMonth(Integer month) {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        cal.add(Calendar.MONTH, -month);
        Date expiryDate = cal.getTime();
        return domandaLavoroRepository.findByDatamodificaAfterAndClosedOrderByDatamodificaDesc(expiryDate,false);
    }

    public List<DomandaLavoro> findByCategoria(String categoria) {
        List<Categoria> categorie = new ArrayList();
        categorie.add(categoriaServiceImpl.findByNome(categoria.trim()));
        return domandaLavoroRepository.findByCategorieOrderByDatamodificaDesc(categorie);
    }

    public Boolean saveDomandaLavoro(DomandaLavoro domandaLavoro) {

        User user = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        domandaLavoro.setUser(user);
        List<Categoria> categorie = domandaLavoro.getCategorie();

        boolean aggiornamento = domandaLavoro.getId() != null
                && domandaLavoroRepository.existsById(domandaLavoro.getId());
        if (aggiornamento) {
            domandaLavoro.setDatamodificaNow();
        } else {
            domandaLavoro.setDatacreazioneNow();
        }

        // Le categorie vengono persistite/ritrovate e assegnate al lato proprietario
        // dell'associazione, poi la domanda viene salvata una sola volta.
        domandaLavoro.setCategorie(categoriaServiceImpl.saveCategorie(categorie, domandaLavoro));
        DomandaLavoro save = domandaLavoroRepository.save(domandaLavoro);

        return save != null;

    }

}
