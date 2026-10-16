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
    public String doChangepw(@Valid @ModelAttribute ChangePwForm form, BindingResult result) {
        String usernameAutenticato = SecurityContextHolder.getContext().getAuthentication().getName();
        if (result.hasErrors() || !usernameAutenticato.equals(form.getUsername())) {
            return "redirect:/user/changepw?errore=Richiesta+non+valida";
        }
        boolean aggiornata = Boolean.TRUE.equals(userDetailsService.ChangePassword(
                usernameAutenticato, form.getPassword(), form.getNew_password(), form.getConfirm_password()));
        return aggiornata
                ? "redirect:/user?alert=Password+aggiornata"
                : "redirect:/user/changepw?errore=Password+non+aggiornata+-+controllare+password+attuale+e+conferma";
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
        String usernameAutenticato = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(usernameAutenticato);
        if (user == null) {
            return "redirect:/login";
        }
        // Identità e stato sono derivati dalla sessione e dal DB, non dal form del browser.
        if (userNoPWRule.getId() == null || !user.getId().equals(userNoPWRule.getId())) {
            return "redirect:/user/edituser?errore=Richiesta+non+valida";
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/user/edituser?errore=Dati+non+validi+-+controllare+i+campi+obbligatori";
        }
        user.setFirstname(userNoPWRule.getFirstname());
        user.setLastname(userNoPWRule.getLastname());
        user.setEmail(userNoPWRule.getEmail());
        if (Boolean.TRUE.equals(userDetailsService.saveNoPw(user))) {
            return "redirect:/user?alert=Account+aggiornato";
        }
        return "redirect:/user/edituser?errore=Account+non+salvato";
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
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);
        if (isProprietarioCommento(loadDomandaLavoroById, loadCommentoById)) {
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
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);
        if (isProprietarioCommento(loadDomandaLavoroById, loadCommentoById)) {
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
     * Verifica proprietà, domanda e ruolo amministrativo prima di permettere modifiche a un
     * commento: gli ID arrivano dalla richiesta e non costituiscono autorizzazione.
     */
    private boolean isProprietarioCommento(DomandaLavoro domanda, Commento commento) {
        if (domanda == null || commento == null || commento.getUser() == null
                || commento.getDomanda() == null
                || !domanda.getId().equals(commento.getDomanda().getId())) {
            return false;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return admin || commento.getUser().getUsername().equals(auth.getName());
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

    @PostMapping(value = "/user/doNewCommento/{id}", produces = {MediaType.TEXT_HTML_VALUE})
    public String doNewCommento(@PathVariable Long id,
                                @RequestParam(name = "descrizione", required = false) String descrizione,
                                Model model) {
        DomandaLavoro domanda = domandaServiceImpl.loadDomandaLavoroById(id);
        if (domanda == null) {
            return "redirect:/user/listdomande?errore=Domanda+non+trovata";
        }

        Commento commento = commentoDaRichiesta(descrizione, Boolean.FALSE);
        if (commento == null) {
            return "redirect:/user/domanda/" + id + "?errore=Il+commento+deve+avere+almeno+8+caratteri";
        }

        commentoServiceImpl.saveCommentoDomanda(domanda.getId(), commento);
        return "redirect:/user/domanda/" + id + "?alert=Commento+salvato";
    }

    @PostMapping(value = "/user/doNewAnswer/{id}", produces = {MediaType.TEXT_HTML_VALUE})
    public String doNewAnswer(@PathVariable Long id,
                              @RequestParam(name = "descrizione", required = false) String descrizione,
                              Model model) {
        DomandaLavoro domanda = domandaServiceImpl.loadDomandaLavoroById(id);
        if (domanda == null) {
            return "redirect:/user/listdomande?errore=Domanda+non+trovata";
        }

        Commento risposta = commentoDaRichiesta(descrizione, Boolean.TRUE);
        if (risposta == null) {
            return "redirect:/user/domanda/" + id + "?errore=La+risposta+deve+avere+almeno+8+caratteri";
        }

        commentoServiceImpl.saveCommentoDomanda(domanda.getId(), risposta);
        return "redirect:/user/domanda/" + id + "?alert=Risposta+salvata";
    }

    @PostMapping(value = "/user/doNewCommentoCommento/{id_dom}/{id_com}", produces = {MediaType.TEXT_HTML_VALUE})
    public String doNewCommentoCommento(@PathVariable("id_dom") Long id_dom, @PathVariable("id_com") Long id_com, @RequestParam(name = "descrizione", required = false) String descrizione, Model model) {
        DomandaLavoro loadDomandaLavoroById = domandaServiceImpl.loadDomandaLavoroById(id_dom);
        Commento loadCommentoById = commentoServiceImpl.loadCommentoById(id_com);
        if (!isProprietarioCommento(loadDomandaLavoroById, loadCommentoById)) {
            return "redirect:/403";
        }
        Commento nuovoCommento = commentoDaRichiesta(descrizione, Boolean.FALSE);
        if (nuovoCommento == null) {
            return "redirect:/user/domanda/" + id_dom + "?errore=Il+commento+deve+avere+almeno+8+caratteri";
        }
        commentoServiceImpl.saveCommentoCommento(id_com, nuovoCommento);
        return "redirect:/user/domanda/" + id_dom + "?alert=Commento+salvato";
    }

    @PostMapping(value = "/user/editcommentocommento/{id_dom}/{id_com}", produces = {MediaType.TEXT_HTML_VALUE})
    public String editcommentocommento(@PathVariable("id_dom") Long idDomanda,
                                       @PathVariable("id_com") Long idCommento,
                                       @RequestParam(name = "descrizione", required = false) String descrizione) {
        DomandaLavoro domanda = domandaServiceImpl.loadDomandaLavoroById(idDomanda);
        Commento commento = commentoServiceImpl.loadCommentoById(idCommento);
        if (!isProprietarioCommento(domanda, commento)) {
            return "redirect:/403";
        }
        Commento aggiornato = commentoDaRichiesta(descrizione, Boolean.FALSE);
        if (aggiornato == null) {
            return "redirect:/user/domanda/" + idDomanda + "?errore=Il+commento+deve+avere+almeno+8+caratteri";
        }
        aggiornato.setId(commento.getId());
        commentoServiceImpl.saveCommentoDomanda(idDomanda, aggiornato);
        return "redirect:/user/domanda/" + idDomanda + "?alert=Commento+aggiornato";
    }

    @RequestMapping(value = "/user/listcommenti/{id}", method = RequestMethod.GET)
    public String listcommenti(Model model, @PathVariable("id") Long idCommento) {
        Commento commento = commentoServiceImpl.loadCommentoById(idCommento);
        List<Commento> commenti;
        if (commento != null && commento.getDomanda() != null) {
            // id di un commento della domanda: mostra i commenti della sua domanda
            model.addAttribute("domanda", commento.getDomanda());
            commenti = commentoServiceImpl.findByDomanda(commento.getDomanda().getId());
        } else {
            // id di un commento genitore: mostra le risposte collegate
            commenti = commentoServiceImpl.findByCommento(idCommento);
        }
        model.addAttribute("commenti", commenti);
        model.addAttribute("commento_id", idCommento);
        return "listcommenti";
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

    // Il flusso web crea domanda e categoria insieme da /user/newdomanda.
    // Endpoint conservato solo per compatibilità; non accetta operazioni privilegiate.
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
        return "redirect:/user/newdomanda?errore=Selezionare+almeno+una+categoria";

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

    // Categoria non viene eliminata tramite un POST browser: la cancellazione avrebbe
    // impatto globale sulle domande e richiede un flusso amministrativo dedicato.
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
    public String listdomandePerCategoria(Model model, @PathVariable("categoria") String categoria) {
        model.addAttribute("domande", domandaServiceImpl.findByCategoria(categoria));
        model.addAttribute("allCategorie", categorieDisponibili());
        model.addAttribute("intestazioneFiltro", "Domande della categoria: " + categoria);
        return "listdomande";
    }

    @RequestMapping(value = "/user/listdomande/user/{user}", method = RequestMethod.GET)
    public String listdomandePerUtente(Model model, @PathVariable("user") String user) {
        model.addAttribute("domande", domandaServiceImpl.findByUser(user));
        model.addAttribute("allCategorie", categorieDisponibili());
        model.addAttribute("intestazioneFiltro", "Domande dell'utente: " + user);
        return "listdomande";
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

        model.addAttribute("allCategorie", categorieDisponibili());
        return "listdomande";

    }

    /**
     * Elenco domande con filtri opzionali passati come parametri di query
     * ({@code /user/listdomande?categoria=...&utente=...}): nessun JavaScript necessario.
     */
    @RequestMapping(value = "/user/listdomande", method = RequestMethod.GET)
    public String listdomande(Model model,
                              @RequestParam(name = "categoria", required = false) String categoria,
                              @RequestParam(name = "utente", required = false) String utente) {
        model.addAttribute("allCategorie", categorieDisponibili());
        if (categoria != null && !categoria.isBlank()) {
            model.addAttribute("domande", domandaServiceImpl.findByCategoria(categoria));
            model.addAttribute("intestazioneFiltro", "Domande della categoria: " + categoria);
        } else if (utente != null && !utente.isBlank()) {
            model.addAttribute("domande", domandaServiceImpl.findByUser(utente));
            model.addAttribute("intestazioneFiltro", "Domande dell'utente: " + utente);
        } else {
            model.addAttribute("domande", domandaServiceImpl.findByMonth(6));
            model.addAttribute("intestazioneFiltro", "Domande degli ultimi 6 mesi");
        }
        return "listdomande";
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
