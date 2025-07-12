package com.gametester.controller;

import com.gametester.repository.EstrategiaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EstrategiaController {

    private final EstrategiaRepository estrategiaRepository;

    public EstrategiaController(EstrategiaRepository estrategiaRepository) {
        this.estrategiaRepository = estrategiaRepository;
    }

    @GetMapping("/estrategias")
    public String listarEstrategias(Model model) {
        model.addAttribute("listaEstrategias", estrategiaRepository.findAll());
        return "estrategias";
    }
}