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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Repository
public interface DomandaLavoroRepository extends JpaRepository<DomandaLavoro, Long> {

    public List<DomandaLavoro> findByUserOrderByDatamodificaDesc(User user);

    public List<DomandaLavoro> findByCategorieOrderByDatamodificaDesc(List<Categoria> categorie);

    public List<DomandaLavoro> findByDatamodificaAfterAndDatamodificaBeforeOrderByDatamodificaDesc(Timestamp timestamp, Timestamp timestamp0);

    public List<DomandaLavoro> findByDatamodificaAfterOrderByDatamodificaDesc(Date expiryDate);

    public List<DomandaLavoro> findByDatamodificaAfterAndClosedOrderByDatamodificaDesc(Date expiryDate, boolean b);

   

}
