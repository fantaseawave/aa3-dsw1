package com.gametester.controller;

import com.gametester.model.Bug;
import com.gametester.model.SessaoTeste;
import com.gametester.repository.BugRepository;
import com.gametester.repository.SessaoTesteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/testador/sessoes/{sessaoId}")
public class BugController {

    private final BugRepository bugRepository;
    private final SessaoTesteRepository sessaoTesteRepository;

    public BugController(BugRepository bugRepository, SessaoTesteRepository sessaoTesteRepository) {
        this.bugRepository = bugRepository;
        this.sessaoTesteRepository = sessaoTesteRepository;
    }

    @GetMapping("/bugs")
    public String listarBugsDaSessao(@PathVariable("sessaoId") Integer sessaoId, Model model) {
        SessaoTeste sessao = sessaoTesteRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de sessão inválido:" + sessaoId));

        model.addAttribute("sessao", sessao);
        model.addAttribute("listaBugs", bugRepository.findBySessaoTesteId(sessaoId));
        return "testador/lista-bugs";
    }

    @GetMapping("/bugs/novo")
    public String mostrarFormularioNovoBug(@PathVariable("sessaoId") Integer sessaoId, Model model, RedirectAttributes redirectAttributes) {
        SessaoTeste sessao = sessaoTesteRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de sessão inválido:" + sessaoId));

        if ("FINALIZADO".equals(sessao.getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível registrar bugs em uma sessão finalizada.");
            return "redirect:/testador/sessoes"; // Redireciona de volta para a lista de sessões
        }

        Bug bug = new Bug();
        bug.setSessaoTeste(sessao);

        model.addAttribute("bug", bug);
        return "testador/formulario-bug";
    }

    @PostMapping("/bugs/salvar")
    public String salvarBug(@PathVariable("sessaoId") Integer sessaoId, @ModelAttribute("bug") Bug bug, RedirectAttributes redirectAttributes) {
        SessaoTeste sessao = sessaoTesteRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de sessão inválido:" + sessaoId));

        if ("FINALIZADO".equals(sessao.getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível registrar bugs em uma sessão finalizada.");
            return "redirect:/testador/sessoes"; // Redireciona de volta para a lista de sessões
        }

        bug.setSessaoTeste(sessao);
        bugRepository.save(bug);

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Bug registrado com sucesso!");
        return "redirect:/testador/sessoes";
    }
}