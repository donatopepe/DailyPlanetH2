/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2.service;

import com.pepedonato.DailyPlanetH2.model.User;
import com.pepedonato.DailyPlanetH2.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 *
 * @author dr.ing. Pepe Donato
 */
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final boolean codepasswordindb = false;

    @Override
    public UserDetails loadUserByUsername(String username) {
        System.out.println("cerco: " + username);
        User user = userRepository.findByUsername(username);
        if ((user == null) || user.getDisabled()) {
            if (user == null) {
                System.out.println("Non trovato: " + username);
            } else {
                System.out.println("Utente disabilitato: " + username);
            }
            throw new UsernameNotFoundException(username);
        }
        System.out.println("trovato: " + user.getUsername() + " pw:" + user.getPassword() + " role: " + user.getUserrole());

        String pwd = null;
        if (codepasswordindb) {
            pwd = user.getPassword();
        } else {
            pwd = passwordEncoder.encode(user.getPassword());
        }

        UserDetails dt = new org.springframework.security.core.userdetails.User(
                username,
                pwd,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getUserrole())));

        System.out.println("role: " + dt.getAuthorities().toString());
        return dt;
    }

    public User loadUserByUsernameAndPassword(String username, String password) {
        System.out.println("cerco: " + username);
        User user = userRepository.findByUsernameAndPassword(username, password);
        if ((user == null) || user.getDisabled()) {
            if (user == null) {
                System.out.println("Non trovato: " + username);
            } else {
                System.out.println("Utente disabilitato: " + username);
            }
            throw new UsernameNotFoundException(username);
        }

        System.out.println("trovato: " + user.getUsername() + " pw:" + user.getPassword() + " role: " + user.getUserrole());

        return user;

    }

    public User loadUserById(Long id) {
        System.out.println("cerco: " + id);
        Optional<User> findById = userRepository.findById(id);
        User user = findById.get();
        if (user == null) {
            System.out.println("Non trovato: " + id);
        } else {
            System.out.println("trovato: " + user.getUsername() + " pw:" + user.getPassword() + " role: " + user.getUserrole());

        }

        return user;

    }

    public Boolean ChangePassword(String username, String password, String new_password, String confirm_password) {
        System.out.println("cerco: " + username);
        User user = userRepository.findByUsernameAndPassword(username, password);
        if ((user == null) || user.getDisabled()) {
            if (user == null) {
                System.out.println("Non trovato: " + username);
            } else {
                System.out.println("Utente disabilitato: " + username);

            }
            return false;
        }
        String pwd = null;
        if (codepasswordindb) {
            pwd = passwordEncoder.encode(password);
        } else {
            pwd = password;
        }
        System.out.println("trovato: " + user.getUsername() + " pw:" + user.getPassword() + " role: " + user.getUserrole());
        if ((pwd.equals(user.getPassword())) && (new_password.equals(confirm_password))) {
            String pwdnew = null;
            if (codepasswordindb) {
                pwdnew = passwordEncoder.encode(new_password);
            } else {
                pwdnew = new_password;
            }
            user.setPassword(pwdnew);
            userRepository.save(user);
            System.out.println("Password cambiata");
            return true;

        } else {
            System.out.println("Passord diverse");
            return false;
        }
    }

    public Boolean AdminChangePassword(String username, String new_password, String confirm_password) {
        System.out.println("cerco: " + username);
        User user = userRepository.findByUsername(username);
        if ((user == null)) {

            System.out.println("Non trovato: " + username);

            return false;
        }
        System.out.println("trovato: " + user.getUsername() + " pw:" + user.getPassword() + " role: " + user.getUserrole());

        if (new_password.equals(confirm_password)) {
            String pwdnew = null;
            if (codepasswordindb) {
                pwdnew = passwordEncoder.encode(new_password);
            } else {
                pwdnew = new_password;
            }
            user.setPassword(pwdnew);
            userRepository.save(user);
            System.out.println("Password cambiata");
            return true;

        } else {
            System.out.println("Passord diverse");
            return false;
        }

    }

    public Boolean save(User user) {
        try {
            String pwdnew = null;
            if (codepasswordindb) {
                pwdnew = passwordEncoder.encode(user.getPassword());
            } else {
                pwdnew = user.getPassword();
            }
            user.setPassword(pwdnew);
            
            userRepository.save(user);
            System.out.println("Account salvato con successo");
            return true;

        } catch (Exception e) {
            System.out.println("Account non salvato su db");
            return false;
        }

    }
    
        public Boolean saveNoPw(User user) {
        try {
           
            userRepository.save(user);
            System.out.println("Account salvato con successo");
            return true;

        } catch (Exception e) {
            System.out.println("Account non salvato su db");
            return false;
        }

    }

    public Boolean existsUserByUsername(String username) {
        System.out.println("cerco: " + username);
        User user = userRepository.findByUsername(username);

        if (user != null) {
            System.out.println("Utente già presente: " + username);
            return true;
        } else {
            System.out.println("Utente non presente: " + username);
            return false;
        }

    }

    public List<User> findAll() {
        return userRepository.findAll(sortBy());
    }

    private Sort sortBy() {
        return new Sort(Sort.Direction.ASC, "lastname");
    }
}
