/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.util.HtmlUtils;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Entity
@Table
public class Categoria implements Serializable {

    /**
     * Lato INVERSO dell'associazione con le domande: il lato proprietario è
     * {@code DomandaLavoro.categorie}. Prima entrambi i lati erano proprietari, con due
     * tabelle di join duplicate ({@code categoria_domande} e {@code domanda_lavoro_categorie}).
     *
     * <p>Le modifiche qui valgono solo in memoria: la persistenza avviene aggiornando
     * {@code DomandaLavoro.categorie}.</p>
     */
    @ManyToMany(mappedBy = "categorie")
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
        }
        if (!domande.contains(domanda)){
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
