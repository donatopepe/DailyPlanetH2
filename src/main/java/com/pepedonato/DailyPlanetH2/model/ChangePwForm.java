/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author dr.ing. Pepe Donato
 */
public class ChangePwForm {

    private String username;

    private String password;
    
    @NotNull(message = "Name cannot be null")
    @Size(min = 8, message = "Your password must at least 8 characters")
    private String new_password;
    private String confirm_password;

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the new_password
     */
    public String getNew_password() {
        return new_password;
    }

    /**
     * @param new_password the new_password to set
     */
    public void setNew_password(String new_password) {
        this.new_password = new_password;
    }

    /**
     * @return the confirm_password
     */
    public String getConfirm_password() {
        return confirm_password;
    }

    /**
     * @param confirm_password the confirm_password to set
     */
    public void setConfirm_password(String confirm_password) {
        this.confirm_password = confirm_password;
    }

}
