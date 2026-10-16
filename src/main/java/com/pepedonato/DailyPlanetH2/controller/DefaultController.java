package com.pepedonato.DailyPlanetH2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pagine pubbliche.
 *
 * <p>La creazione dell'account amministratore iniziale è stata spostata in
 * {@link com.pepedonato.DailyPlanetH2.config.AdminAccountInitializer}: prima avveniva
 * qui con credenziali scritte nel codice (credenziale di default rimossa dal repository
 * e dalla storia git).</p>
 */
@Controller
public class DefaultController {

    @GetMapping("/403")
    public String error403() {
        return "error/403";
    }

    @GetMapping("/")
    public String home1() {
        return "about";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/home")
    public String home() {
        return "about";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

}
