/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.service;

import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.CommentoRepository;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Service
public class CommentoServiceImpl {

    @Autowired
    private CommentoRepository commentoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DomandaServiceImpl domandaServiceImpl;

    public List<Commento> findByUser(String name) {
        User user = userRepository.findByUsername(name);
        return commentoRepository.findByUserOrderByDatamodificaDesc(user);
    }

    public Commento loadCommentoById(Long id) {
        Optional<Commento> findById = commentoRepository.findById(id);
        return findById.get();
    }

    public List<Commento> findByDomandaRisposta(Long domanda_id, Boolean risposta) {

        return commentoRepository.findByDomandaAndRispostaOrderByDatamodificaAsc(domandaServiceImpl.loadDomandaLavoroById(domanda_id), risposta);
    }

    public List<Commento> findByDomanda(Long domanda_id) {

        return commentoRepository.findByDomandaOrderByDatamodificaAsc(domandaServiceImpl.loadDomandaLavoroById(domanda_id));
    }

    public List<Commento> findByCommento(Long commento_id) {

        return commentoRepository.findByCommentoOrderByDatamodificaAsc(this.loadCommentoById(commento_id));
    }

    public Boolean saveCommentoDomanda(Long domanda_id, Commento commento) {

        User user = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        commento.setUser(user);
        commento.setDomanda(domandaServiceImpl.loadDomandaLavoroById(domanda_id));

        if ((commento.getId() != null) && commentoRepository.existsById(commento.getId())) {
            commento.setDatacreazione(loadCommentoById(commento.getId()).getDatacreazione());
            commento.setDatamodificaNow();

        } else {
            commento.setDatacreazioneNow();
        }

        Commento save = commentoRepository.save(commento);
        return save != null;

    }

    public Boolean saveCommentoCommento(Long commento_id, Commento commento) {

        User user = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        commento.setUser(user);
        commento.setCommento(this.loadCommentoById(commento_id));

        if ((commento.getId() != null) && commentoRepository.existsById(commento.getId())) {

            commento.setDatacreazione(loadCommentoById(commento.getId()).getDatacreazione());
            commento.setDatamodificaNow();

        } else {
            commento.setDatacreazioneNow();
        }

        Commento save = commentoRepository.save(commento);
        return save != null;

    }

}
