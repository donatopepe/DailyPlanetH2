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
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
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

        model.addAttribute("user", user);
        if (userDetailsService.existsUserByUsername(user.getUsername())) {
            System.out.println("username already present");
            model.addAttribute("alert", "Username already present");
            return "newaccountDo :: results";
        } else {
            if (bindingResult.hasErrors()) {
                System.out.println("bindind ha errori");
                model.addAttribute("alert", "Error Binding");
                return "newaccountDo :: results";
            } else {
                Boolean save = userDetailsService.save(user);

                if (save) {
                    System.out.println("Ok account salvato");
                    model.addAttribute("alert", "Account Saved");
                } else {
                    System.out.println("Account non salvato");
                    model.addAttribute("alert", "Account not Saved");
                }
            }
        }

        return "newaccountDo :: results";
    }

    @GetMapping("/admin/changepw/{id}")
    public ModelAndView adminchangepw(@PathVariable Long id, ModelAndView mav) {
        //ModelAndView mav = new ModelAndView();
        User user = userDetailsService.loadUserById(id);
        ChangePwForm changePwForm = new ChangePwForm();
        changePwForm.setUsername(user.getUsername());

        mav.addObject("ChangePwForm", changePwForm);

        mav.setViewName("adminchangepw");
        System.out.println("Lancio id:" + user.getId());

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

            System.out.println("Lancio id:" + user.getId());
            model.addAttribute("userNoPW", userNoPW);

            return "editaccount";
        } else {
            return "redirect:/admin/account";
        }

    }

    @PostMapping("/admin/doAdminChangepw")
    public String doAdminChangepw(@Valid
            @ModelAttribute ChangePwForm ChangePwForm, BindingResult bindingResult, Model model, ModelAndView mav) {

        mav.addObject("ChangePwForm", ChangePwForm);
        System.out.println("username:" + ChangePwForm.getUsername());

        if (bindingResult.hasErrors()) {
            System.out.println("bindind ha errori");
            model.addAttribute("alert", "Error Binding");

            return "adminchangepwDo :: resultschangepw";
        } else {

            Boolean save = userDetailsService.AdminChangePassword(ChangePwForm.getUsername(), ChangePwForm.getNew_password(), ChangePwForm.getConfirm_password());

            if (save) {
                System.out.println("Ok Password salvato");
                model.addAttribute("alert", "Password Saved");
                return "redirectaccount";
            } else {
                System.out.println("Password non salvato");
                model.addAttribute("alert", "Password not Saved");
                return "adminchangepwDo :: resultschangepw";
            }
        }

    }

    @PostMapping("/admin/doEditAccount")
    public String doEditAccount(@Valid @ModelAttribute("userNoPW") UserNoPW userNoPW, BindingResult bindingResult, Model model, HttpServletRequest request) {

        model.addAttribute("userNoPW", userNoPW);
        User user = userDetailsService.loadUserById(userNoPW.getId());

        if (!(user.getUsername().equals(userNoPW.getUsername())) && userDetailsService.existsUserByUsername(userNoPW.getUsername())) {
            System.out.println("username already present");
            model.addAttribute("alert", "Username already present");

        } else {

            if (bindingResult.hasErrors()) {
                System.out.println("bindind ha errori");
                model.addAttribute("alert", "Error Binding");

            } else {

                user.setUsername(userNoPW.getUsername());
                user.setFirstname(userNoPW.getFirstname());
                user.setLastname(userNoPW.getLastname());
                user.setEmail(userNoPW.getEmail());
                user.setUserrole(userNoPW.getUserrole());
                user.setDisabled(userNoPW.getDisabled());

                Boolean save = userDetailsService.saveNoPw(user);

                if (save) {
                    System.out.println("Ok account salvato");
                    model.addAttribute("alert", "Account Saved");
                    return "redirectaccount";
                } else {
                    System.out.println("Account non salvato");
                    model.addAttribute("alert", "Account not Saved");
                }

            }
        }

        return "editaccountDo :: results";
    }

}
