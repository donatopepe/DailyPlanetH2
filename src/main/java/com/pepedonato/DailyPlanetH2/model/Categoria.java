/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.web.util.HtmlUtils;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Entity
@Table
public class Categoria implements Serializable {

    @ManyToMany
    private List<DomandaLavoro> domande;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotNull(message = "Name cannot be null")
    @Size(min = 3, message = "Your category name must at least 3 characters")
    private String nome;


    public void addDomanda(DomandaLavoro domanda) {
        if (domande == null) {
            domande = new ArrayList();
            System.out.println("Istanzio lista somande in categoria perchè non instanziata");
        }
        if (!domande.contains(domanda)){
            System.out.println("Addizziono domanda che non esiste in lista della categoria");
            domande.add(domanda);
        }
        
        //comment.setDomandaLavoro(this);
    }
 
    public void removeDomanda(DomandaLavoro domanda) {
        domande.remove(domanda);
        //comment.setDomandaLavoro(null);
        
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the nome
     */
    public String getNome() {
        return nome;
    }

    /**
     * @param nome the nome to set
     */
    public void setNome(String nome) {
//        this.nome=HtmlUtils.htmlEscape(nome.trim());
        this.nome=nome.trim();
    }

    /**
     * @return the domande
     */
    public List<DomandaLavoro> getDomande() {
        return domande;
    }

    /**
     * @param domande the domande to set
     */
    public void setDomande(List<DomandaLavoro> domande) {
        this.domande = domande;
    }


}
