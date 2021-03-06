/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.controller;

import com.pepedonato.DailyPlanetH2.CommandLineRunnerBean;
import com.pepedonato.DailyPlanetH2.model.Categoria;
import com.pepedonato.DailyPlanetH2.model.ChangePwForm;
import com.pepedonato.DailyPlanetH2.model.Commento;
import com.pepedonato.DailyPlanetH2.model.DomandaLavoro;
import com.pepedonato.DailyPlanetH2.model.DomandaView;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.model.UserNoPWRule;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import com.pepedonato.DailyPlanetH2.service.CategoriaServiceImpl;
import com.pepedonato.DailyPlanetH2.service.CommentoServiceImpl;
import com.pepedonato.DailyPlanetH2.service.DomandaServiceImpl;
import com.pepedonato.DailyPlanetH2.service.MyUserDetailsService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Donato
 */
@Controller
public class UserController {
    @Autowired
    private MyUserDetailsService userDetailsService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoriaServiceImpl categoriaServiceImpl;
    @Autowired
    private DomandaServiceImpl domandaServiceImpl;
    @Autowired
    private CommentoServiceImpl commentoServiceImpl;

    private static final Logger logger = LoggerFactory.getLogger(CommandLineRunnerBean.class);
    private ArrayList<String> allCategorie;
    private ArrayList<String> allUser;

    @GetMapping("/user")
    public String user(Model model) {
        allCategorie = new ArrayList();
        categoriaServiceImpl.getAll().forEach((categoria2) -> {

            allCategorie.add(categoria2.getNome());
            //  System.out.println("Categorie: " + categoria2.getNome());

        });
        model.addAttribute("allCategorie", allCategorie);
        allUser = new ArrayList();
        userDetailsService.findAll().forEach((user2) -> {
            allUser.add(user2.getUsername());
            //System.out.println(user2.getUsername());
        });
        model.addAttribute("allUser", allUser);
        return "user";
    }

    @GetMapping("/user/changepw")
    public ModelAndView changepw(ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();
        ChangePwForm changePwForm = new ChangePwForm();
        changePwForm.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        mav.addObject("ChangePwForm", changePwForm);

        mav.setViewName("changepw");

        return mav;

    }

    @PostMapping("/user/doChangepw")
    public ModelAndView doChangepw(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute ChangePwForm ChangePwForm, ModelAndView mav,
            BindingResult result) throws ServletException {
        //ModelAndView mav = new ModelAndView();

        System.out.println(ChangePwForm.getUsername());
        System.out.println(ChangePwForm.getPassword());
        System.out.println(ChangePwForm.getNew_password());
        System.out.println(ChangePwForm.getConfirm_password());
        Boolean ChangePassword = userDetailsService.ChangePassword(ChangePwForm.getUsername(), ChangePwForm.getPassword(), ChangePwForm.getNew_password(), ChangePwForm.getConfirm_password());

        if (ChangePassword) {
            System.out.println("Cambio Password eseguito");
            mav.setViewName("redirectuser");
            mav.addObject("alert", "Change password successfully");
            return mav;
        } else {
            System.out.println("Cambio Password fallito");
            mav.addObject("ChangePwForm", ChangePwForm);
            mav.addObject("alert", "Change password unsuccessfully");
            mav.setViewName("changepwDo :: resultschangepw");

            return mav;
        }

    }

    @GetMapping("/user/edituser")
    public String edituser(Model model, ModelAndView mav) {

        User user = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user != null) {
            UserNoPWRule userNoPWRule = new UserNoPWRule();
            userNoPWRule.setId(user.getId());
            userNoPWRule.setUsername(user.getUsername());
            userNoPWRule.setFirstname(user.getFirstname());
            userNoPWRule.setLastname(user.getLastname());
            userNoPWRule.setEmail(user.getEmail());

            userNoPWRule.setDisabled(user.getDisabled());

            System.out.println("Lancio id:" + user.getId());
            model.addAttribute("userNoPWRule", userNoPWRule);

            return "edituser";
        } else {
            return "redirect:/";
        }

    }

    @PostMapping("/user/doEditUser")
    public String doEditUser(@Valid @ModelAttribute("userNoPWRule") UserNoPWRule userNoPWRule, BindingResult bindingResult, Model model, HttpServletRequest request) {

        model.addAttribute("userNoPWRule", userNoPWRule);
        User user = userDetailsService.loadUserById(userNoPWRule.getId());
        if (!(user.getUsername().equals(userNoPWRule.getUsername())) && userDetailsService.existsUserByUsername(userNoPWRule.getUsername())) {
            System.out.println("username already present");
            model.addAttribute("alert", "Username already present");
            return "edituserDo :: results";
        } else {

            if (bindingResult.hasErrors()) {
                System.out.println("bindind ha errori");
                model.addAttribute("alert", "Error Binding");

            } else {

                user.setUsername(userNoPWRule.getUsername());
                user.setFirstname(userNoPWRule.getFirstname());
                user.setLastname(userNoPWRule.getLastname());
                user.setEmail(userNoPWRule.getEmail());

                user.setDisabled(userNoPWRule.getDisabled());

                Boolean save = userDetailsService.save(user);

                if (save) {
                    System.out.println("Ok account salvato");
                    model.addAttribute("alert", "Account Saved");
                    return "redirectuser";
                } else {
                    System.out.println("Account non salvato");
                    model.addAttribute("alert", "Account not Saved");
                }

            }
        }

        return "edituserDo :: results";
    }

    @GetMapping("/user/domanda/{id}")
    public String domandaview(@PathVariable Long id, Model model, ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id);
        if (loadDomandaLavoroById != null) {
            System.out.println("Lancio domanda id:" + id);
            model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id));
            //model.addAttribute("categorie", loadDomandaLavoroById.getCategorie()); //funziona ma non vengono messi in ordine alfabetico
            model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id, Boolean.FALSE));
            model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id, Boolean.TRUE));
            model.addAttribute("domanda", loadDomandaLavoroById);
            Commento newcomment = new Commento();
            newcomment.setRisposta(false);
            model.addAttribute("newcomment", newcomment);
            Commento newanswer = new Commento();
            newanswer.setRisposta(true);
            model.addAttribute("newanswer", newanswer);
            model.addAttribute("newcommentcomment", new Commento());

            return "domanda";
        } else {
            return "redirect:/user";
        }

    }

    @GetMapping("/user/editcommento/{id_dom}/{id_com}")
    public String editcommento(@PathVariable("id_dom") Long id_dom, @PathVariable("id_com") Long id_com, Model model, ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);
        if ((loadDomandaLavoroById != null) && (loadCommentoById != null) && (loadCommentoById.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName()))) {
            System.out.println("Lancio domanda id:" + id_dom);
            model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id_dom));
            //model.addAttribute("categorie", loadDomandaLavoroById.getCategorie()); //funziona ma non vengono messi in ordine alfabetico
            model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.FALSE));
            model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.TRUE));
            model.addAttribute("domanda", loadDomandaLavoroById);
            model.addAttribute("newcomment", loadCommentoById);
            model.addAttribute("newanswer", new Commento());
            model.addAttribute("newcommentcomment", new Commento());

            return "domanda";
        } else {
            return "redirect:/user";
        }

    }

    @GetMapping("/user/editrisposta/{id_dom}/{id_com}")
    public String editrisposta(@PathVariable("id_dom") Long id_dom, @PathVariable("id_com") Long id_com, Model model, ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);
        if ((loadDomandaLavoroById != null) && (loadCommentoById != null) && (loadCommentoById.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName()))) {
            System.out.println("Lancio domanda id:" + id_dom);
            model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id_dom));
            //model.addAttribute("categorie", loadDomandaLavoroById.getCategorie()); //funziona ma non vengono messi in ordine alfabetico
            model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.FALSE));
            model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.TRUE));
            model.addAttribute("domanda", loadDomandaLavoroById);
            model.addAttribute("newcomment", new Commento());
            model.addAttribute("newanswer", loadCommentoById);
            model.addAttribute("newcommentcomment", new Commento());

            return "domanda";
        } else {
            return "redirect:/user";
        }

    }

    @PostMapping(value = "/user/doNewCommento/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String doNewCommento(@PathVariable Long id, @Valid @ModelAttribute("newcomment") Commento commento, BindingResult bindingResult, Model model, HttpServletRequest request) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id);
        if (bindingResult.hasErrors()) {
            System.out.println("bindind ha errori " + bindingResult.toString());
            model.addAttribute("alert", "Error Binding");
            if (loadDomandaLavoroById != null) {
                System.out.println("Lancio domanda id:" + id);
                model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id));
                //model.addAttribute("categorie", loadDomandaLavoroById.getCategorie()); //funziona ma non vengono messi in ordine alfabetico
                model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id, Boolean.FALSE));
                model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id, Boolean.TRUE));
                model.addAttribute("domanda", loadDomandaLavoroById);
                model.addAttribute("newcomment", commento);
                model.addAttribute("newanswer", new Commento());
                model.addAttribute("newcommentcomment", new Commento());

                return "newcommentoDo :: resultsnewcommento";
            }

        } else {
            System.out.println("Provo a salvare commento per domanda id:" + id);

            if (loadDomandaLavoroById != null) {

                commento.setRisposta(Boolean.FALSE);
                if (commentoServiceImpl.saveCommentoDomanda(loadDomandaLavoroById.getId(), commento)) {
                    System.out.println("Ok commento salvato: " + commento.getDescrizione());
                }

            }
        }
        return "redirectuser";

    }

    @PostMapping(value = "/user/doNewAnswer/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String doNewAnswer(@PathVariable Long id, @Valid @ModelAttribute("newanswer") Commento commento, BindingResult bindingResult, Model model, HttpServletRequest request) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id);
        if (bindingResult.hasErrors()) {
            System.out.println("bindind ha errori " + bindingResult.toString());
            model.addAttribute("alert", "Error Binding");
            if (loadDomandaLavoroById != null) {
                System.out.println("Lancio domanda id:" + id);
                model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id));
                //model.addAttribute("categorie", loadDomandaLavoroById.getCategorie()); //funziona ma non vengono messi in ordine alfabetico
                model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id, Boolean.FALSE));
                model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id, Boolean.TRUE));
                model.addAttribute("domanda", loadDomandaLavoroById);
                model.addAttribute("newcomment", new Commento());
                model.addAttribute("newanswer", commento);
                model.addAttribute("newcommentcomment", new Commento());
                model.addAttribute("newanswercomment", new Commento());

                return "newcommentoDo :: resultsnewcommento";
            }
        } else {
            System.out.println("Provo a salvare commento per domanda id:" + id);

            if (loadDomandaLavoroById != null) {

                commento.setRisposta(Boolean.TRUE);
                if (commentoServiceImpl.saveCommentoDomanda(loadDomandaLavoroById.getId(), commento)) {
                    System.out.println("Ok commento salvato: " + commento.getDescrizione());
                }

            }
        }
        return "redirectuser";

    }

    @PostMapping(value = "/user/doNewCommentoCommento/{id_dom}/{id_com}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String doNewCommentoCommento(@PathVariable("id_dom") Long id_dom, @PathVariable("id_com") Long id_com, @Valid @ModelAttribute("newcommentcomment") Commento commento, BindingResult bindingResult, Model model, HttpServletRequest request) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);
        if (bindingResult.hasErrors()) {
            System.out.println("bindind ha errori " + bindingResult.toString());
            model.addAttribute("alert", "Error Binding");
            if (loadCommentoById != null) {
                System.out.println("Lancio domanda id:" + id_dom);
                model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id_dom));
                //model.addAttribute("categorie", loadDomandaLavoroById.getCategorie()); //funziona ma non vengono messi in ordine alfabetico
                model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.FALSE));
                model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.TRUE));
                model.addAttribute("domanda", loadDomandaLavoroById);
                model.addAttribute("newcomment", new Commento());
                model.addAttribute("newanswer", new Commento());
                model.addAttribute("newcommentcomment", commento);
                model.addAttribute("newanswercomment", new Commento());

                return "newcommentoDo :: resultsnewcommento";
            }

        } else {
            System.out.println("Provo a salvare commento per domanda id:" + id_dom + " comment id:" + id_com);

            if (loadCommentoById != null) {

                commento.setRisposta(Boolean.FALSE);
                if (commentoServiceImpl.saveCommentoCommento(loadCommentoById.getId(), commento)) {
                    System.out.println("Ok commento salvato: " + commento.getDescrizione());
                }

            }
        }
        return "redirectuser";

    }

    @PostMapping(value = "/user/editcommentocommento/{id_dom}/{id_com}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String editcommentocommento(@PathVariable("id_dom") Long id_dom, @PathVariable("id_com") Long id_com, @Valid @ModelAttribute("newcommentcomment") Commento commento, BindingResult bindingResult, Model model, HttpServletRequest request) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);

        System.out.println("Lancio domanda id:" + id_dom);
        model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id_dom));
        //model.addAttribute("categorie", loadDomandaLavoroById.getCategorie()); //funziona ma non vengono messi in ordine alfabetico
        model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.FALSE));
        model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id_dom, Boolean.TRUE));
        model.addAttribute("domanda", loadDomandaLavoroById);
        model.addAttribute("newcomment", new Commento());
        model.addAttribute("newanswer", new Commento());
        model.addAttribute("newcommentcomment", loadCommentoById);
        model.addAttribute("newanswercomment", new Commento());

        return "newcommentoDo :: resultsnewcommento";

    }

    @RequestMapping(value = "/user/listcommenti/{id}", method = RequestMethod.GET)
    public String listdomandeuser(Model model, @PathVariable("id") Long id) {
        model.addAttribute("commentiper", commentoServiceImpl.findByCommento(id));
        model.addAttribute("commento_id", id);
        return "listcommentDo :: results";
    }

    @GetMapping("/user/newdomanda")
    public String newdomanda(Model model, ModelAndView mav) {

        allCategorie = new ArrayList();
        categoriaServiceImpl.getAll().forEach((categoria2) -> {

            allCategorie.add(categoria2.getNome());
//            System.out.println("Categorie: " + categoria2.getNome());

        });
        model.addAttribute("allCategorie", allCategorie);
        DomandaView domanda = new DomandaView();
        model.addAttribute("domanda", domanda);

        return "newdomanda";
    }

    @RequestMapping(value = "/user/newdomandaDo", method = RequestMethod.GET)
    //public String newdomandaDo(Model model, @RequestBody String json) throws Exception {
    public String newdomandaDo(@ModelAttribute("domanda") DomandaView domanda, Model model) throws Exception {
        model.addAttribute("domanda", domanda);
        model.addAttribute("allCategorie", allCategorie);

        return "newdomandaDo :: resultsnewdomanda";

    }

//    @RequestMapping(value = "/user/doNewDomanda", method = RequestMethod.POST, params = "action=Save Category")
    @PostMapping(value = "/user/doNewDomanda/SaveCategory", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String doSaveCategoria(@Valid @ModelAttribute("domanda") DomandaView domanda, BindingResult bindingResult, Model model, HttpServletRequest request) {

        /*
        String referer = request.getHeader("Referer");
        System.out.println("referer:" + referer);
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            //   map.put(key, value);
            System.out.println(key + ":" + value);
        }
         */
 /*
        if (bindingResult.hasErrors()) {
            System.out.println("bindind ha errori");
            model.addAttribute("alert", "Error Binding");

        } else 
         */
        {
            System.out.println("Aggiungi domanda>: " + domanda.getTitolo());
            System.out.println("Titolo>:" + domanda.getTitolo());
            System.out.println("Descrizione>:" + domanda.getDescrizione());
            System.out.println("Categoria>:" + domanda.getCategoria());
            domanda.setCategoria(domanda.getCategoria());
            System.out.println("Categorie>:" + domanda.getCategorie());

            /*
            domanda.setCategorie(categorie);
           
            if (domandaServiceImpl.saveDomandaLavoro(domanda)) {
                System.out.println("Salvataggio domanda eseguito con successo");
                mav.setViewName("redirect:/user");
                return mav;
            } else {
                System.out.println("Slavataggio domanda non eseguito");
            }
             */
        }

        model.addAttribute("allCategorie", allCategorie);
        model.addAttribute("domanda", domanda);
        return "newdomandaDo :: resultsnewdomanda";

    }

    @PostMapping(value = "/user/doNewDomanda/SaveQuestion", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String doNewDomanda(@Valid @ModelAttribute("domanda") DomandaView domanda, BindingResult bindingResult, Model model, HttpServletRequest request) throws Exception {

        if (bindingResult.hasErrors()) {
            System.out.println("bindind ha errori " + bindingResult.toString());
            model.addAttribute("alert", "Error Binding");

        } else {
            System.out.println("Aggiungi domanda>: " + domanda.getTitolo());
            System.out.println("Titolo>:" + domanda.getTitolo());
            System.out.println("Descrizione>:" + domanda.getDescrizione());
            System.out.println("Categoria>:" + domanda.getCategoria());
            domanda.setCategoria(domanda.getCategoria());
            System.out.println("Categorie>:" + domanda.getCategorie());
            System.out.println("Numero categorie:" + domanda.getCategorie().size());
            if ((domanda.getCategorie() != null) && (domanda.getCategorie().size() > 0)) {
                DomandaLavoro domandalavoro = new DomandaLavoro();
                List<Categoria> categorie = new ArrayList();
                for (String elemento : domanda.getCategorie()) {
                    if (elemento.trim().length() > 0) {
                        // dovrei cercare se categoria già esiste e non crearne una nuova 
                        Categoria categoria = new Categoria();
                        categoria.setNome(elemento);
                        categorie.add(categoria);
                        System.out.println("Inserisco in domanda lavoro categoria:" + elemento);
                    }
                }
                domandalavoro.setCategorie(categorie);
                domandalavoro.setId(domanda.getId_domanda());
                domandalavoro.setTitolo(domanda.getTitolo());
                domandalavoro.setDescrizione(domanda.getDescrizione());
                domandalavoro.setClosed(domanda.getClosed());
                if (domandaServiceImpl.saveDomandaLavoro(domandalavoro)) {
                    System.out.println("Salvataggio domanda eseguito con successo");
                    model.addAttribute("alert", "Save success");
                    return "redirectuser";
                } else {
                    System.out.println("Slavataggio domanda non eseguito");
                    model.addAttribute("alert", "Save Error");
                }
            } else {
                System.out.println("Inserire almeno una categoria");
                model.addAttribute("alert", "Enter at least one category");

            }

        }

        model.addAttribute("allCategorie", allCategorie);
        model.addAttribute("domanda", domanda);

        return "newdomandaDo :: resultsnewdomanda";
    }

    @PostMapping(value = "/user/doNewDomanda/DelCategory", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String doDelCategoria(@Valid @ModelAttribute("domanda") DomandaView domanda, BindingResult bindingResult, Model model, HttpServletRequest request) {

        System.out.println("Aggiungi domanda>: " + domanda.getTitolo());
        System.out.println("Titolo>:" + domanda.getTitolo());
        System.out.println("Descrizione>:" + domanda.getDescrizione());
        System.out.println("Categoria>:" + domanda.getCategoria());
        System.out.println("Categoria da cancellare>:" + domanda.getId_cat());
        domanda.delCategoria(domanda.getId_cat());
        System.out.println("Categorie>:" + domanda.getCategorie());

        model.addAttribute("allCategorie", allCategorie);
        model.addAttribute("domanda", domanda);

        return "newdomandaDo :: resultsnewdomanda";
    }

    @GetMapping("/user/editdomanda/{id}")
    public String editdomanda(@PathVariable Long id, Model model, ModelAndView mav) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id);
        if (loadDomandaLavoroById == null) {
            System.out.println("Domanda non trovata");
        } else {
            if (loadDomandaLavoroById.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {

                System.out.println("Edit domanda id: " + id);

                DomandaView domandaView = new DomandaView();
                domandaView.setId_domanda(id);
                domandaView.setTitolo(loadDomandaLavoroById.getTitolo());
                domandaView.setDescrizione(loadDomandaLavoroById.getDescrizione());
                domandaView.setClosed(loadDomandaLavoroById.getClosed());
                for (Categoria categoria : loadDomandaLavoroById.getCategorie()) {
                    domandaView.setCategoria(categoria.getNome());
                }

                allCategorie = new ArrayList();
                categoriaServiceImpl.getAll().forEach((categoria2) -> {

                    allCategorie.add(categoria2.getNome());
                    //System.out.println("Categorie: " + categoria2.getNome());

                });
                model.addAttribute("allCategorie", allCategorie);
                model.addAttribute("domanda", domandaView);

                return "newdomanda";
            }
            System.out.println("Errore utente");
        }

        return "redirect:/user";
    }


    @RequestMapping(value = "/user/listdomande/category/{categoria}", method = RequestMethod.GET)
    public String listdomande(Model model, @PathVariable("categoria") String categoria) {
        model.addAttribute("domande", domandaServiceImpl.findByCategoria(categoria));

        return "listdomande :: resultsList";
    }

    @RequestMapping(value = "/user/listdomande/user/{user}", method = RequestMethod.GET)
    public String listdomandeuser(Model model, @PathVariable("user") String user) {
        System.out.println("Cerco domande by user:" + user);
        model.addAttribute("domande", domandaServiceImpl.findByUser(user));
        return "listdomande :: resultsList";
    }

    @RequestMapping(value = "/user/listdomande/date", method = RequestMethod.POST)
    public String listdomandedate(Model model, @RequestBody Map<String, Object> json) throws Exception {

        System.out.println("Date start :" + json.get("datestart"));

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date end = df.parse(json.get("dateend").toString());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(end);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date date = calendar.getTime();
        System.out.println("data end:" + date.toString());
        model.addAttribute("domande", domandaServiceImpl.findByDates(df.parse(json.get("datestart").toString()), date));

        return "listdomande :: resultsList";

    }

    @RequestMapping(value = "/user/listdomande", method = RequestMethod.GET)
    public String listdomande(Model model) {
        // model.addAttribute("domande", domandaServiceImpl.getAll());
        model.addAttribute("domande", domandaServiceImpl.findByMonth(6));
        return "listdomande :: resultsList";
    }
    /*
    private void ProvaDomanda() {
        DomandaLavoro domandalavoro = new DomandaLavoro();

        domandalavoro.setTitolo("Titolo domanda 2 di prova");
        domandalavoro.setDescrizione(domandalavoro.getTitolo() + " descrizione");

        List<Categoria> categorie = new ArrayList();
        Categoria categoria = new Categoria();
        categoria.setNome("prova categoria domanda 1");
        categorie.add(categoria);
        categoria = new Categoria();
        categoria.setNome("prova categoria domanda 2");
        categorie.add(categoria);
        categoria = new Categoria();
        categoria.setNome("prova categoria domanda 3");
        categorie.add(categoria);
        domandalavoro.setCategorie(categorie);
        System.out.println("Provo a salvare");
        if (domandaServiceImpl.saveDomandaLavoro(domandalavoro)) {
            System.out.println("Salvataggio domanda eseguito con successo");
        } else {
            System.out.println("Slavataggio domanda non eseguito");
        }

        Commento commento = new Commento();
        commento.setDescrizione("Primo commento");
        commento.setRisposta(Boolean.FALSE);
        if (commentoServiceImpl.saveCommentoDomanda(domandalavoro.getId(), commento)) {
            System.out.println("Ok commento salvato: " + commento.getDescrizione());
        }

        System.out.println("Ricerca in base alla categoria");
        domandaServiceImpl.findByCategoria(categoria.getNome()).forEach((domanda) -> {
            System.out.println("domanda: " + domanda.getId() + " " + domanda.getTitolo());
            commentoServiceImpl.findByDomandaRisposta(domanda.getId(), Boolean.FALSE).forEach((commento2) -> {
                System.out.println("^^^^ Commento: " + commento2.getId() + " " + commento2.getDescrizione());
            });
        });
        System.out.println("Ricerca in base all'utente attuale");
        domandaServiceImpl.findByUser(SecurityContextHolder.getContext().getAuthentication().getName()).forEach((domanda) -> {
            System.out.println("domanda: " + domanda.getId() + " " + domanda.getTitolo());
        });
        commentoServiceImpl.findByUser(SecurityContextHolder.getContext().getAuthentication().getName()).forEach((commento2) -> {
            System.out.println("^^^^ Risposta:" + commento2.getRisposta() + " " + commento2.getId() + " " + commento2.getDescrizione());
        });
        System.out.println("Cerco categorie in base a domanda attuale");

        categoriaServiceImpl.findByDomanda(domandalavoro.getId()).forEach((categoria2) -> {
            System.out.println("Trovata categoria " + categoria2.getNome());
        });
    }
     */
}
