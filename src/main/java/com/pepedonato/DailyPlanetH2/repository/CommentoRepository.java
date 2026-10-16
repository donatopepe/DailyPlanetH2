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
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository dei commenti e delle risposte.
 *
 * <p>Con {@code spring.jpa.open-in-view: false} le collezioni e le associazioni usate
 * dai template ({@code user}, {@code commento} genitore) vengono caricate con
 * {@link EntityGraph} nella stessa query.</p>
 *
 * @author dr.ing. Pepe Donato
 */
@Repository
public interface CommentoRepository extends JpaRepository<Commento, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "commento"})
    List<Commento> findAll();

    @EntityGraph(attributePaths = {"user", "commento"})
    public List<Commento> findByUserOrderByDatamodificaDesc(User user);

    @EntityGraph(attributePaths = {"user", "commento"})
    public List<Commento> findByDomandaAndRispostaOrderByDatamodificaAsc(DomandaLavoro loadDomandaLavoroById, Boolean risposta);

    @EntityGraph(attributePaths = {"user", "commento"})
    public List<Commento> findByCommentoOrderByDatamodificaAsc(Commento loadCommentoById);

    @EntityGraph(attributePaths = {"user", "commento"})
    public List<Commento> findByDomandaOrderByDatamodificaAsc(DomandaLavoro loadDomandaLavoroById);

}
