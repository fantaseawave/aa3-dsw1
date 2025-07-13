package com.gametester.controller;

import com.gametester.repository.EstrategiaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping("/estrategias/{id}") // Novo método para detalhes da estratégia
    public String detalhesEstrategia(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("estrategia", estrategiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de estratégia inválido:" + id)));
        return "testador/detalhes-estrategia"; // Novo template
    }
}