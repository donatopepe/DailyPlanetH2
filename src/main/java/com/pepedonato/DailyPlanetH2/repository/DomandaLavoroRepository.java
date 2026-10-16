/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.repository;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.User;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository delle domande di lavoro.
 *
 * <p>Le collezioni usate dalle viste ({@code categorie}) e l'autore ({@code user}) sono
 * caricate con {@link EntityGraph}: l'applicazione usa
 * {@code spring.jpa.open-in-view: false}, quindi tutto ciò che serve al rendering dei
 * template Thymeleaf deve essere recuperato nella stessa query.</p>
 *
 * @author dr.ing. Pepe Donato
 */
@Repository
public interface DomandaLavoroRepository extends JpaRepository<DomandaLavoro, Long> {

    @Override
    @EntityGraph(attributePaths = {"categorie", "user"})
    List<DomandaLavoro> findAll();

    @EntityGraph(attributePaths = {"categorie", "user"})
    public List<DomandaLavoro> findByUserOrderByDatamodificaDesc(User user);

    @EntityGraph(attributePaths = {"categorie", "user"})
    public List<DomandaLavoro> findByCategorieOrderByDatamodificaDesc(List<Categoria> categorie);

    @EntityGraph(attributePaths = {"categorie", "user"})
    public List<DomandaLavoro> findByDatamodificaAfterAndDatamodificaBeforeOrderByDatamodificaDesc(Timestamp timestamp, Timestamp timestamp0);

    @EntityGraph(attributePaths = {"categorie", "user"})
    public List<DomandaLavoro> findByDatamodificaAfterOrderByDatamodificaDesc(Date expiryDate);

    @EntityGraph(attributePaths = {"categorie", "user"})
    public List<DomandaLavoro> findByDatamodificaAfterAndClosedOrderByDatamodificaDesc(Date expiryDate, boolean b);

}
