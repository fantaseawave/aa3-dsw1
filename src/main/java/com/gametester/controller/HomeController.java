package com.gametester.controller;

import com.gametester.model.Usuario;
import com.gametester.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;

    public HomeController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) { // Adicionado Model
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            Usuario usuarioLogado = usuarioRepository.findByEmail(email).orElse(null); // Buscar o usuário completo
            model.addAttribute("usuarioLogado", usuarioLogado); // Adicionar ao Model

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