/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.repository;

import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {



    public List<Categoria> findByDomandeOrderByNomeAsc(DomandaLavoro domanda);

    public Categoria findByNomeIgnoreCase(String nome);


   
}
