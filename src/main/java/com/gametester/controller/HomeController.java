package com.gametester.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                if (auth.getAuthority().equals("ADMINISTRADOR")) {
                    return "redirect:/admin/dashboard";
                }
                if (auth.getAuthority().equals("TESTADOR")) {
                    return "redirect:/testador/dashboard";
                }
            }
        }
        return "redirect:/login";
    }
}