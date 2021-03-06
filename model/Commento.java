/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.model;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
public class Commento implements Serializable {

    @ManyToOne
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Timestamp datacreazione;
    @Column
    private Timestamp datamodifica;

    @Column(nullable = false, columnDefinition="LONGTEXT")
    @NotNull(message = "Name cannot be null")
    @Size(min = 8, message = "Your description must at least 8 characters")
    private String descrizione;

    @Column
    private Boolean risposta;       //se true non è un commento alla domanda ma una risposta

    @OneToOne
    private Commento commento;

    @OneToOne
    private DomandaLavoro domanda;

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
     * @return the descrizione
     */
    public String getDescrizione() {
        /*
        if (this.descrizione!=null){
            return HtmlUtils.htmlEscape(descrizione);  
        }
        */
        return descrizione;
    //    return HtmlUtils.htmlEscape(descrizione);

    }

    /**
     * @param descrizione the descrizione to set
     */
    public void setDescrizione(String descrizione) {
//        this.descrizione = HtmlUtils.htmlEscape(descrizione);
        this.descrizione = descrizione;
    }

    /**
     * @return the risposta
     */
    public Boolean getRisposta() {
        return risposta;
    }

    /**
     * @param risposta the risposta to set
     */
    public void setRisposta(Boolean risposta) {
        this.risposta = risposta;
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
     * @return the commento
     */
    public Commento getCommento() {
        return commento;
    }

    /**
     * @param commento the commento to set
     */
    public void setCommento(Commento commento) {
        this.commento = commento;
    }

    /**
     * @return the domanda
     */
    public DomandaLavoro getDomanda() {
        return domanda;
    }

    /**
     * @param domanda the domanda to set
     */
    public void setDomanda(DomandaLavoro domanda) {
        this.domanda = domanda;
    }

}
