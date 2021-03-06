/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.model;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.web.util.HtmlUtils;

/**
 *
 * @author dr.ing. Pepe Donato
 */
public class DomandaView {

    @NotNull(message = "Title cannot be null")
    @Size(min = 8, message = "Your title must at least 8 characters")
    private String titolo;

    @NotNull(message = "Description cannot be null")
    @Size(min = 8, message = "Your description must at least 8 characters")
    private String descrizione;

    private String categoria;

    private Integer id_cat;

    private Long id_domanda;

    private Boolean closed = false;

    private List<String> categorie = new ArrayList();

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }
    public Long getId_domanda() {
        return id_domanda;
    }

    public void setId_domanda(Long id_domanda) {
        this.id_domanda = id_domanda;
    }

    public Integer getId_cat() {
        return id_cat;
    }

    public void setId_cat(Integer id_cat) {
        this.id_cat = id_cat;
    }

    public void delCategoria(int id) {

        categorie.remove(id);
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
     * @return the categoria
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * @param categoria the categoria to set
     */
    public void setCategoria(String categoria) {
        if ((categoria != null) && (categoria.trim().length() > 0)) {
//            this.categoria = HtmlUtils.htmlEscape(categoria.trim());
            this.categoria = categoria.trim();
            //if (categorie==null){ categorie = new ArrayList();}
            Boolean present = false;
            for (String element : categorie) {
                if (element.equalsIgnoreCase(this.categoria)) {
                    present = true;
                }
            }
            if (!present) {
                categorie.add(categoria);
            }
        }
    }

    public List<String> getCategorie() {
        return categorie;
    }

    public void setCategorie(List<String> categorie) {
        this.categorie = categorie;
    }

}
