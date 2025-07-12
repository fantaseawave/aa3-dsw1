package com.gametester.controller;

import com.gametester.model.SessaoTeste;
import com.gametester.model.Usuario;
import com.gametester.repository.EstrategiaRepository;
import com.gametester.repository.ProjetoRepository;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Timestamp;

@Controller
@RequestMapping("/testador")
public class TestadorController {

    private final UsuarioRepository usuarioRepository;
    private final ProjetoRepository projetoRepository;
    private final SessaoTesteRepository sessaoTesteRepository;
    private final EstrategiaRepository estrategiaRepository;

    public TestadorController(UsuarioRepository usuarioRepository, ProjetoRepository projetoRepository, SessaoTesteRepository sessaoTesteRepository, EstrategiaRepository estrategiaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.projetoRepository = projetoRepository;
        this.sessaoTesteRepository = sessaoTesteRepository;
        this.estrategiaRepository = estrategiaRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "testador/dashboard";
    }

    @GetMapping("/projetos")
    public String meusProjetos(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario testador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário testador não encontrado na base de dados."));
        model.addAttribute("listaProjetos", projetoRepository.findByMembros_Id(testador.getId()));
        return "testador/meus-projetos";
    }

    @GetMapping("/sessoes")
    public String minhasSessoes(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario testador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário testador não encontrado na base de dados."));
        model.addAttribute("listaSessoes", sessaoTesteRepository.findByTestadorId(testador.getId()));
        return "testador/minhas-sessoes";
    }

    @GetMapping("/estrategias")
    public String visualizarEstrategias(Model model) {
        model.addAttribute("listaEstrategias", estrategiaRepository.findAll());
        return "testador/estrategias";
    }

    @GetMapping("/sessoes/nova")
    public String mostrarFormularioNovaSessao(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario testador = usuarioRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("Testador não encontrado."));
        SessaoTeste sessaoTeste = new SessaoTeste();
        sessaoTeste.setTestador(testador);
        model.addAttribute("sessaoTeste", sessaoTeste);
        model.addAttribute("listaProjetos", projetoRepository.findByMembros_Id(testador.getId()));
        model.addAttribute("listaEstrategias", estrategiaRepository.findAll());
        return "testador/formulario-sessao";
    }

    @PostMapping("/sessoes/salvar")
    public String salvarSessao(@ModelAttribute("sessaoTeste") SessaoTeste sessaoTeste, Authentication authentication, RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        Usuario testador = usuarioRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("Testador não encontrado."));
        sessaoTeste.setTestador(testador);
        sessaoTeste.setStatus("CRIADO");
        sessaoTesteRepository.save(sessaoTeste);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Sessão de teste criada com sucesso!");
        return "redirect:/testador/sessoes";
    }

    @GetMapping("/sessoes/iniciar/{id}")
    public String iniciarSessao(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        SessaoTeste sessao = sessaoTesteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Sessão inválida:" + id));
        sessao.setStatus("EM_EXECUCAO");
        sessao.setDataHoraInicio(new Timestamp(System.currentTimeMillis()));
        sessaoTesteRepository.save(sessao);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Sessão iniciada!");
        return "redirect:/testador/sessoes";
    }

    @GetMapping("/sessoes/finalizar/{id}")
    public String finalizarSessao(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        SessaoTeste sessao = sessaoTesteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Sessão inválida:" + id));
        sessao.setStatus("FINALIZADO");
        sessao.setDataHoraFim(new Timestamp(System.currentTimeMillis()));
        sessaoTesteRepository.save(sessao);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Sessão finalizada com sucesso.");
        return "redirect:/testador/sessoes";
    }
}