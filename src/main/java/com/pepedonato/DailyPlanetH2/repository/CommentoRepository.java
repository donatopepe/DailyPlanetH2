/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.repository;

import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Repository
public interface CommentoRepository extends JpaRepository<Commento, Long> {

    public List<Commento> findByUserOrderByDatamodificaDesc(User user);

    public List<Commento> findByDomandaAndRispostaOrderByDatamodificaAsc(DomandaLavoro loadDomandaLavoroById, Boolean risposta);

    public List<Commento> findByCommentoOrderByDatamodificaAsc(Commento loadCommentoById);

    public List<Commento> findByDomandaOrderByDatamodificaAsc(DomandaLavoro loadDomandaLavoroById);

    
    
    
}
