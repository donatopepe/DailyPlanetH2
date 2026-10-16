/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.controller;

import com.pepedonato.DailyPlanetH2.model.ChangePwForm;
import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.model.UserNoPW;
import com.pepedonato.DailyPlanetH2.service.MyUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Donato
 */
@Controller
public class AdminController {

    @Autowired
    private MyUserDetailsService userDetailsService;

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/admin/newaccount")
    public ModelAndView newaccount(ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();

        mav.addObject("user", new User());

        mav.setViewName("newaccount");

        return mav;

    }

    @GetMapping("/admin/account")
    public ModelAndView account(ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();

        mav.addObject("listaUser", userDetailsService.findAll());

        mav.setViewName("account");

        return mav;

    }

    @PostMapping("/admin/doSaveAccount")
    public String doSaveAccount(@Valid @ModelAttribute("user") User user, BindingResult bindingResult, Model model, HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            return "redirect:/admin/newaccount?errore=Dati+non+validi";
        }
        if (user.getUserrole() == null || !List.of("USER", "ADMIN").contains(user.getUserrole())) {
            return "redirect:/admin/newaccount?errore=Ruolo+non+valido";
        }
        if (userDetailsService.existsUserByUsername(user.getUsername())) {
            return "redirect:/admin/newaccount?errore=Nome+utente+già+presente";
        }
        if (Boolean.TRUE.equals(userDetailsService.save(user))) {
            return "redirect:/admin/account?alert=Account+creato";
        }
        return "redirect:/admin/newaccount?errore=Account+non+salvato";
    }

    @GetMapping("/admin/changepw/{id}")
    public ModelAndView adminchangepw(@PathVariable Long id, ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();
        User user = userDetailsService.loadUserById(id);
        ChangePwForm changePwForm = new ChangePwForm();
        changePwForm.setId(user.getId());
        changePwForm.setUsername(user.getUsername());

        mav.addObject("ChangePwForm", changePwForm);

        mav.setViewName("adminchangepw");

        return mav;

    }

    @GetMapping("/admin/edit/{id}")
    public String admineditaccount(@PathVariable Long id, Model model, ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();
        User user = userDetailsService.loadUserById(id);
        if (user != null) {
            UserNoPW userNoPW = new UserNoPW();
            userNoPW.setId(user.getId());
            userNoPW.setUsername(user.getUsername());
            userNoPW.setFirstname(user.getFirstname());
            userNoPW.setLastname(user.getLastname());
            userNoPW.setEmail(user.getEmail());
            userNoPW.setUserrole(user.getUserrole());
            userNoPW.setDisabled(user.getDisabled());

            model.addAttribute("userNoPW", userNoPW);

            return "editaccount";
        } else {
            return "redirect:/admin/account";
        }

    }

    @PostMapping("/admin/doAdminChangepw")
    public String doAdminChangepw(@Valid @ModelAttribute ChangePwForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors() || form.getId() == null) {
            return "redirect:/admin/account?errore=Password+non+valida";
        }
        User target = userDetailsService.loadUserById(form.getId());
        if (target == null) {
            return "redirect:/admin/account?errore=Account+non+trovato";
        }
        boolean save = Boolean.TRUE.equals(userDetailsService.AdminChangePassword(
                target.getUsername(), form.getNew_password(), form.getConfirm_password()));
        return save
                ? "redirect:/admin/account?alert=Password+reimpostata"
                : "redirect:/admin/account?errore=Password+non+reimpostata+-+le+due+password+devono+coincidere";
    }

    @PostMapping("/admin/doEditAccount")
    public String doEditAccount(@Valid @ModelAttribute("userNoPW") UserNoPW userNoPW, BindingResult bindingResult, Model model, HttpServletRequest request) {

        if (userNoPW.getId() == null) {
            return "redirect:/admin/account?errore=Account+non+trovato";
        }
        User user = userDetailsService.loadUserById(userNoPW.getId());
        if (user == null) {
            return "redirect:/admin/account?errore=Account+non+trovato";
        }

        if (!(user.getUsername().equals(userNoPW.getUsername()))
                && userDetailsService.existsUserByUsername(userNoPW.getUsername())) {
            return "redirect:/admin/edit/" + userNoPW.getId() + "?errore=Nome+utente+già+presente";
        }
        if (bindingResult.hasErrors()) {
            return "redirect:/admin/edit/" + userNoPW.getId() + "?errore=Dati+non+validi";
        }
        // Mai permettere al client di impostare privilegi o stato da un DTO non validato.
        if (!List.of("USER", "ADMIN").contains(userNoPW.getUserrole())) {
            return "redirect:/admin/edit/" + userNoPW.getId() + "?errore=Ruolo+non+valido";
        }
        user.setUsername(userNoPW.getUsername());
        user.setFirstname(userNoPW.getFirstname());
        user.setLastname(userNoPW.getLastname());
        user.setEmail(userNoPW.getEmail());
        user.setUserrole(userNoPW.getUserrole());
        user.setDisabled(Boolean.TRUE.equals(userNoPW.getDisabled()));

        if (Boolean.TRUE.equals(userDetailsService.saveNoPw(user))) {
            return "redirect:/admin/account?alert=Account+aggiornato";
        }
        return "redirect:/admin/edit/" + userNoPW.getId() + "?errore=Account+non+salvato";
    }

}
