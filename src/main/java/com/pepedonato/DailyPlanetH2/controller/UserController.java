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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * Categorie disponibili per i form. Costruita ad ogni richiesta: prima erano campi
     * di istanza mutati dai metodi (stato condiviso tra richieste concorrenti).
     */
    private ArrayList<String> categorieDisponibili() {
        ArrayList<String> categorie = new ArrayList<>();
        for (Categoria categoria : categoriaServiceImpl.getAll()) {
            categorie.add(categoria.getNome());
        }
        return categorie;
    }

    @GetMapping("/user")
    public String user(Model model) {
        model.addAttribute("allCategorie", categorieDisponibili());

        ArrayList<String> allUser = new ArrayList<>();
        for (User utente : userDetailsService.findAll()) {
            allUser.add(utente.getUsername());
        }
        model.addAttribute("allUser", allUser);
        return "user";
    }

    @GetMapping("/user/changepw")
    public ModelAndView changepw(ModelAndView mav) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        ChangePwForm changePwForm = new ChangePwForm();
        changePwForm.setUsername(name);
        mav.addObject("ChangePwForm", changePwForm);
        // true quando l'accesso e' forzato dall'interceptor (mustChangePassword)
        mav.addObject("forzato", userDetailsService.isPasswordChangeRequired(name));

        mav.setViewName("changepw");

        return mav;

    }

    @PostMapping("/user/doChangepw")
    public ModelAndView doChangepw(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute ChangePwForm ChangePwForm, ModelAndView mav,
            BindingResult result) throws ServletException {
        //ModelAndView mav = new ModelAndView();

        Boolean ChangePassword = userDetailsService.ChangePassword(ChangePwForm.getUsername(), ChangePwForm.getPassword(), ChangePwForm.getNew_password(), ChangePwForm.getConfirm_password());

        if (ChangePassword) {
            mav.setViewName("redirect:/user");
            mav.addObject("alert", "Change password successfully");
            return mav;
        } else {
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
            model.addAttribute("alert", "Username already present");
            return "edituserDo :: results";
        } else {

            if (bindingResult.hasErrors()) {
                model.addAttribute("alert", "Error Binding");

            } else {

                user.setUsername(userNoPWRule.getUsername());
                user.setFirstname(userNoPWRule.getFirstname());
                user.setLastname(userNoPWRule.getLastname());
                user.setEmail(userNoPWRule.getEmail());

                user.setDisabled(userNoPWRule.getDisabled());

                // L'utente arriva dal database: la password è già un hash BCrypt.
                // saveNoPw evita il doppio encoding (difetto della versione originale).
                Boolean save = userDetailsService.saveNoPw(user);

                if (save) {
                    model.addAttribute("alert", "Account Saved");
                    return "redirect:/user";
                } else {
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
    /**
     * Costruisce il commento da salvare leggendo solo il testo della richiesta.
     *
     * <p>Non si fa più il binding diretto dell'entità: su rotte come
     * {@code /user/doNewCommento/{id}} Spring legherebbe la variabile di percorso
     * {@code id} al campo {@code id} del {@code Commento}, facendo credere a Hibernate
     * che si tratti di una riga già esistente (errore "Row was updated or deleted by
     * another transaction").</p>
     *
     * @return il commento, oppure {@code null} se il testo non rispetta il minimo richiesto
     */
    private Commento commentoDaRichiesta(String testo, boolean risposta) {
        if (testo == null || testo.trim().length() < 8) {
            return null;
        }
        Commento commento = new Commento();
        commento.setDescrizione(testo.trim());
        commento.setRisposta(risposta);
        return commento;
    }

    /** Aggiunge al model i dati della domanda usati dal frammento {@code newcommentoDo}. */
    private void aggiungiDatiDomanda(Model model, DomandaLavoro domanda) {
        Long id = domanda.getId();
        model.addAttribute("categorie", categoriaServiceImpl.findByDomanda(id));
        model.addAttribute("commenti", commentoServiceImpl.findByDomandaRisposta(id, Boolean.FALSE));
        model.addAttribute("risposte", commentoServiceImpl.findByDomandaRisposta(id, Boolean.TRUE));
        model.addAttribute("domanda", domanda);
        model.addAttribute("newcomment", new Commento());
        model.addAttribute("newanswer", new Commento());
        model.addAttribute("newcommentcomment", new Commento());
        model.addAttribute("newanswercomment", new Commento());
    }

    @PostMapping(value = "/user/doNewCommento/{id}", produces = {MediaType.TEXT_HTML_VALUE})
    public String doNewCommento(@PathVariable Long id,
                                @RequestParam(name = "descrizione", required = false) String descrizione,
                                Model model) {
        DomandaLavoro domanda = domandaServiceImpl.loadDomandaLavoroById(id);
        Commento commento = commentoDaRichiesta(descrizione, Boolean.FALSE);

        if (commento == null) {
            model.addAttribute("alert", "Il commento deve avere almeno 8 caratteri");
            if (domanda != null) {
                aggiungiDatiDomanda(model, domanda);
                return "newcommentoDo :: resultsnewcommento";
            }
            return "redirect:/user";
        }

        if (domanda != null) {
            commentoServiceImpl.saveCommentoDomanda(domanda.getId(), commento);
        }
        return "redirect:/user";
    }

    @PostMapping(value = "/user/doNewAnswer/{id}", produces = {MediaType.TEXT_HTML_VALUE})
    public String doNewAnswer(@PathVariable Long id,
                              @RequestParam(name = "descrizione", required = false) String descrizione,
                              Model model) {
        DomandaLavoro domanda = domandaServiceImpl.loadDomandaLavoroById(id);
        Commento risposta = commentoDaRichiesta(descrizione, Boolean.TRUE);

        if (risposta == null) {
            model.addAttribute("alert", "La risposta deve avere almeno 8 caratteri");
            if (domanda != null) {
                aggiungiDatiDomanda(model, domanda);
                return "newcommentoDo :: resultsnewcommento";
            }
            return "redirect:/user";
        }

        if (domanda != null) {
            commentoServiceImpl.saveCommentoDomanda(domanda.getId(), risposta);
        }
        return "redirect:/user";
    }

    @PostMapping(value = "/user/doNewCommentoCommento/{id_dom}/{id_com}", produces = {MediaType.TEXT_HTML_VALUE})
    public String doNewCommentoCommento(@PathVariable("id_dom") Long id_dom, @PathVariable("id_com") Long id_com, @Valid @ModelAttribute("newcommentcomment") Commento commento, BindingResult bindingResult, Model model, HttpServletRequest request) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);
        if (bindingResult.hasErrors()) {
            model.addAttribute("alert", "Error Binding");
            if (loadCommentoById != null) {
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

            if (loadCommentoById != null) {

                commento.setRisposta(Boolean.FALSE);
                if (commentoServiceImpl.saveCommentoCommento(loadCommentoById.getId(), commento)) {
                }

            }
        }
        return "redirect:/user";

    }

    @PostMapping(value = "/user/editcommentocommento/{id_dom}/{id_com}", produces = {MediaType.TEXT_HTML_VALUE})
    public String editcommentocommento(@PathVariable("id_dom") Long id_dom, @PathVariable("id_com") Long id_com, @Valid @ModelAttribute("newcommentcomment") Commento commento, BindingResult bindingResult, Model model, HttpServletRequest request) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);

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

        model.addAttribute("allCategorie", categorieDisponibili());
        DomandaView domanda = new DomandaView();
        model.addAttribute("domanda", domanda);

        return "newdomanda";
    }

    @RequestMapping(value = "/user/newdomandaDo", method = RequestMethod.GET)
    //public String newdomandaDo(Model model, @RequestBody String json) throws Exception {
    public String newdomandaDo(@ModelAttribute("domanda") DomandaView domanda, Model model) throws Exception {
        model.addAttribute("domanda", domanda);
        model.addAttribute("allCategorie", categorieDisponibili());

        return "newdomandaDo :: resultsnewdomanda";

    }

//    @RequestMapping(value = "/user/doNewDomanda", method = RequestMethod.POST, params = "action=Save Category")
    @PostMapping(value = "/user/doNewDomanda/SaveCategory", produces = {MediaType.TEXT_HTML_VALUE})
    public String doSaveCategoria(@Valid @ModelAttribute("domanda") DomandaView domanda, BindingResult bindingResult, Model model, HttpServletRequest request) {

        /*
        String referer = request.getHeader("Referer");
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            //   map.put(key, value);
        }
         */
 /*
        if (bindingResult.hasErrors()) {
            model.addAttribute("alert", "Error Binding");

        } else 
         */
        {
            domanda.setCategoria(domanda.getCategoria());

            /*
            domanda.setCategorie(categorie);
           
            if (domandaServiceImpl.saveDomandaLavoro(domanda)) {
                mav.setViewName("redirect:/user");
                return mav;
            } else {
            }
             */
        }

        model.addAttribute("allCategorie", categorieDisponibili());
        model.addAttribute("domanda", domanda);
        return "newdomandaDo :: resultsnewdomanda";

    }

    @PostMapping(value = "/user/doNewDomanda/SaveQuestion", produces = {MediaType.TEXT_HTML_VALUE})
    public String doNewDomanda(@Valid @ModelAttribute("domanda") DomandaView domanda, BindingResult bindingResult, Model model, HttpServletRequest request) throws Exception {

        if (bindingResult.hasErrors()) {
            model.addAttribute("alert", "Error Binding");

        } else {
            domanda.setCategoria(domanda.getCategoria());
            if ((domanda.getCategorie() != null) && (domanda.getCategorie().size() > 0)) {
                DomandaLavoro domandalavoro = new DomandaLavoro();
                List<Categoria> categorie = new ArrayList();
                for (String elemento : domanda.getCategorie()) {
                    if (elemento.trim().length() > 0) {
                        // dovrei cercare se categoria già esiste e non crearne una nuova 
                        Categoria categoria = new Categoria();
                        categoria.setNome(elemento);
                        categorie.add(categoria);
                    }
                }
                domandalavoro.setCategorie(categorie);
                domandalavoro.setId(domanda.getId_domanda());
                domandalavoro.setTitolo(domanda.getTitolo());
                domandalavoro.setDescrizione(domanda.getDescrizione());
                domandalavoro.setClosed(domanda.getClosed());
                if (domandaServiceImpl.saveDomandaLavoro(domandalavoro)) {
                    model.addAttribute("alert", "Save success");
                    return "redirect:/user";
                } else {
                    model.addAttribute("alert", "Save Error");
                }
            } else {
                model.addAttribute("alert", "Enter at least one category");

            }

        }

        model.addAttribute("allCategorie", categorieDisponibili());
        model.addAttribute("domanda", domanda);

        return "newdomandaDo :: resultsnewdomanda";
    }

    @PostMapping(value = "/user/doNewDomanda/DelCategory", produces = {MediaType.TEXT_HTML_VALUE})
    public String doDelCategoria(@Valid @ModelAttribute("domanda") DomandaView domanda, BindingResult bindingResult, Model model, HttpServletRequest request) {

        domanda.delCategoria(domanda.getId_cat());

        model.addAttribute("allCategorie", categorieDisponibili());
        model.addAttribute("domanda", domanda);

        return "newdomandaDo :: resultsnewdomanda";
    }

    @GetMapping("/user/editdomanda/{id}")
    public String editdomanda(@PathVariable Long id, Model model, ModelAndView mav) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id);
        if (loadDomandaLavoroById == null) {
        } else {
            if (loadDomandaLavoroById.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {


                DomandaView domandaView = new DomandaView();
                domandaView.setId_domanda(id);
                domandaView.setTitolo(loadDomandaLavoroById.getTitolo());
                domandaView.setDescrizione(loadDomandaLavoroById.getDescrizione());
                domandaView.setClosed(loadDomandaLavoroById.getClosed());
                for (Categoria categoria : loadDomandaLavoroById.getCategorie()) {
                    domandaView.setCategoria(categoria.getNome());
                }

                model.addAttribute("allCategorie", categorieDisponibili());
                model.addAttribute("domanda", domandaView);

                return "newdomanda";
            }
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
        model.addAttribute("domande", domandaServiceImpl.findByUser(user));
        return "listdomande :: resultsList";
    }

    @RequestMapping(value = "/user/listdomande/date", method = RequestMethod.POST)
    public String listdomandedate(Model model, @RequestBody Map<String, Object> json) throws Exception {


        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date end = df.parse(json.get("dateend").toString());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(end);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date date = calendar.getTime();
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
        if (domandaServiceImpl.saveDomandaLavoro(domandalavoro)) {
        } else {
        }

        Commento commento = new Commento();
        commento.setDescrizione("Primo commento");
        commento.setRisposta(Boolean.FALSE);
        if (commentoServiceImpl.saveCommentoDomanda(domandalavoro.getId(), commento)) {
        }

        domandaServiceImpl.findByCategoria(categoria.getNome()).forEach((domanda) -> {
            commentoServiceImpl.findByDomandaRisposta(domanda.getId(), Boolean.FALSE).forEach((commento2) -> {
            });
        });
        domandaServiceImpl.findByUser(SecurityContextHolder.getContext().getAuthentication().getName()).forEach((domanda) -> {
        });
        commentoServiceImpl.findByUser(SecurityContextHolder.getContext().getAuthentication().getName()).forEach((commento2) -> {
        });

        categoriaServiceImpl.findByDomanda(domandalavoro.getId()).forEach((categoria2) -> {
        });
    }
     */
}
