package com.pepedonato.DailyPlanetH2.controller;

import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DefaultController {

    @Autowired
    private MyUserDetailsService userDetailsService;
    private Boolean controlloadmin = false;

    @GetMapping("/403")
    public String error403() {
        return "error/403";
    }

    @GetMapping("/")
    public String home1() {
        //   ProveCategoria();

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
        if (!controlloadmin) {
            if (!userDetailsService.existsUserByUsername("administrator")) {
                User user = new User();
                user.setUsername("administrator");
                user.setPassword("administrator");
                user.setFirstname("administrator");
                user.setLastname("administrator");
                user.setEmail("email@email.com");
                user.setUserrole("ADMIN");
                controlloadmin = userDetailsService.save(user);
            } else {
                controlloadmin = true;
            }
        }
        return "login";
    }

}
