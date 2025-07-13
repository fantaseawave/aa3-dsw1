package com.gametester.controller;

import com.gametester.model.SessaoTeste;
import com.gametester.model.Usuario;
import com.gametester.repository.EstrategiaRepository;
import com.gametester.repository.ProjetoRepository;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.repository.UsuarioRepository;
import com.gametester.service.FileStorageService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.util.Optional;

@Controller
@RequestMapping("/testador")
public class TestadorController {

    private final UsuarioRepository usuarioRepository;
    private final ProjetoRepository projetoRepository;
    private final SessaoTesteRepository sessaoTesteRepository;
    private final EstrategiaRepository estrategiaRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public TestadorController(UsuarioRepository usuarioRepository, ProjetoRepository projetoRepository, SessaoTesteRepository sessaoTesteRepository, EstrategiaRepository estrategiaRepository, PasswordEncoder passwordEncoder, FileStorageService fileStorageService) {
        this.usuarioRepository = usuarioRepository;
        this.projetoRepository = projetoRepository;
        this.sessaoTesteRepository = sessaoTesteRepository;
        this.estrategiaRepository = estrategiaRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    private void addAuthenticatedUserToModel(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            Usuario usuarioLogado = usuarioRepository.findByEmail(email).orElse(null);
            model.addAttribute("usuarioLogado", usuarioLogado);
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        return "testador/dashboard";
    }

    @GetMapping("/projetos")
    public String meusProjetos(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        String email = authentication.getName();
        Usuario testador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário testador não encontrado na base de dados."));
        model.addAttribute("listaProjetos", projetoRepository.findByMembros_Id(testador.getId()));
        return "testador/meus-projetos";
    }

    @GetMapping("/sessoes")
    public String minhasSessoes(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        String email = authentication.getName();
        Usuario testador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário testador não encontrado na base de dados."));
        model.addAttribute("listaSessoes", sessaoTesteRepository.findByTestadorId(testador.getId()));
        return "testador/minhas-sessoes";
    }

    @GetMapping("/estrategias")
    public String visualizarEstrategias(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("listaEstrategias", estrategiaRepository.findAll());
        return "testador/estrategias";
    }

    @GetMapping("/sessoes/nova")
    public String mostrarFormularioNovaSessao(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
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
        if (sessaoTeste.getTempoSessaoMinutos() <= 0) {
            redirectAttributes.addFlashAttribute("mensagemErro", "O tempo da sessão deve ser maior que 0 minutos.");
            return "redirect:/testador/sessoes/nova";
        }

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

    @GetMapping("/meu-perfil")
    public String meuPerfil(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        String email = authentication.getName();
        Usuario testador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário testador não encontrado."));
        testador.setSenha("");
        model.addAttribute("usuario", testador);
        return "testador/meu-perfil";
    }

    @PostMapping("/salvar-perfil")
    public String salvarPerfil(@ModelAttribute("usuario") Usuario usuarioDoFormulario,
                               @RequestParam(value = "profilePictureFile", required = false) MultipartFile profilePictureFile,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        Usuario usuarioLogadoNoBanco = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário testador não encontrado."));

        usuarioLogadoNoBanco.setNome(usuarioDoFormulario.getNome());
        Optional<Usuario> outroUsuarioComEmail = usuarioRepository.findByEmail(usuarioDoFormulario.getEmail());
        if (outroUsuarioComEmail.isPresent() && outroUsuarioComEmail.get().getId() != usuarioLogadoNoBanco.getId()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro: O e-mail '" + usuarioDoFormulario.getEmail() + "' já está em uso por outro usuário.");
            return "redirect:/testador/meu-perfil";
        }
        usuarioLogadoNoBanco.setEmail(usuarioDoFormulario.getEmail());

        if (usuarioDoFormulario.getSenha() != null && !usuarioDoFormulario.getSenha().isEmpty()) {
            usuarioLogadoNoBanco.setSenha(passwordEncoder.encode(usuarioDoFormulario.getSenha()));
        }

        if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
            String nomeDoFicheiro = fileStorageService.storeFile(profilePictureFile);
            usuarioLogadoNoBanco.setProfilePictureUrl("/uploads/" + nomeDoFicheiro);
        } else if (usuarioDoFormulario.getProfilePictureUrl() != null && usuarioDoFormulario.getProfilePictureUrl().isEmpty()) {
            usuarioLogadoNoBanco.setProfilePictureUrl(null);
        }

        usuarioRepository.save(usuarioLogadoNoBanco);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Perfil atualizado com sucesso!");
        return "redirect:/testador/meu-perfil";
    }
}