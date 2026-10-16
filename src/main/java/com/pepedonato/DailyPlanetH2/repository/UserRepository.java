/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.repository;

import com.pepedonato.DailyPlanetH2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>{

   /**
    * Ritrova un utente per username.
    *
    * <p>Non esiste alcun metodo di ricerca per password: le password sono hash BCrypt
    * e vengono verificate con {@code PasswordEncoder.matches}, mai confrontate in SQL.</p>
    */
   User findByUsername(String username);

}
