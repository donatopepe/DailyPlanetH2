/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
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
public class DomandaLavoro implements Serializable {

    @ManyToOne
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Timestamp datacreazione;
    private Timestamp datamodifica;

    @Column(nullable = false)
    @NotNull(message = "Title cannot be null")
    @Size(min = 8, message = "Your title must at least 8 characters")
    @Size(max = 254, message = "Your title must at most 254 characters")
    private String titolo;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @NotNull(message = "Description cannot be null")
    @Size(min = 8, message = "Your description must at least 8 characters")
    private String descrizione;

    @ManyToMany
    @JoinTable
    private List<Categoria> categorie;

    @Column
    private Boolean closed = false;

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public void addCategoria(Categoria categoria) {
        categorie.add(categoria);
        categoria.addDomanda(this);
    }

    public void removeCategoria(Categoria categoria) {
        categorie.remove(categoria);
        categoria.removeDomanda(this);
    }

    public void setDatacreazioneNow() {

        long time = System.currentTimeMillis();
        java.sql.Timestamp timestamp = new java.sql.Timestamp(time);

        this.datacreazione = timestamp;
        this.datamodifica = timestamp;

    }

    public void setDatamodificaNow() {
        long time = System.currentTimeMillis();
        java.sql.Timestamp timestamp = new java.sql.Timestamp(time);

        this.datamodifica = timestamp;
    }

    public DomandaLavoro() {
        super();
        setDatacreazioneNow();
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
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
     * @return the datacreazione
     */
    public Timestamp getDatacreazione() {
        return datacreazione;
    }

    /**
     * @param datacreazione the datacreazione to set
     */
    public void setDatacreazione(Timestamp datacreazione) {
        this.datacreazione = datacreazione;
    }

    /**
     * @return the datamodifica
     */
    public Timestamp getDatamodifica() {
        return datamodifica;
    }

    /**
     * @param datamodifica the datamodifica to set
     */
    public void setDatamodifica(Timestamp datamodifica) {
        this.datamodifica = datamodifica;
    }

    /**
     * @return the titolo
     */
    public String getTitolo() {
        return titolo;
    }

    /**
     * @param titolo the titolo to set
     */
    public void setTitolo(String titolo) {
//        this.titolo = HtmlUtils.htmlEscape(titolo);
        this.titolo = titolo;
    }

    /**
     * @return the descrizione
     */
    public String getDescrizione() {
        return descrizione;
    }

    /**
     * @param descrizione the descrizione to set
     */
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    /**
     * @return the categorie
     */
    public List<Categoria> getCategorie() {
        return categorie;
    }

    /**
     * @param categorie the categorie to set
     */
    public void setCategorie(List<Categoria> categorie) {
        this.categorie = categorie;
    }

}
