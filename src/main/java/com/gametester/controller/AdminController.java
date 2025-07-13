package com.gametester.controller;

import com.gametester.model.Estrategia;
import com.gametester.model.Projeto;
import com.gametester.model.SessaoTeste;
import com.gametester.model.Usuario;
import com.gametester.repository.BugRepository;
import com.gametester.repository.EstrategiaRepository;
import com.gametester.repository.ProjetoRepository;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.repository.UsuarioRepository;
import com.gametester.service.FileStorageService;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final ProjetoRepository projetoRepository;
    private final EstrategiaRepository estrategiaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessaoTesteRepository sessaoTesteRepository;
    private final BugRepository bugRepository;
    private final FileStorageService fileStorageService;

    public AdminController(UsuarioRepository usuarioRepository, ProjetoRepository projetoRepository, EstrategiaRepository estrategiaRepository, PasswordEncoder passwordEncoder, SessaoTesteRepository sessaoTesteRepository, BugRepository bugRepository, FileStorageService fileStorageService) {
        this.usuarioRepository = usuarioRepository;
        this.projetoRepository = projetoRepository;
        this.estrategiaRepository = estrategiaRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessaoTesteRepository = sessaoTesteRepository;
        this.bugRepository = bugRepository;
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
        return "admin/dashboard";
    }

    @GetMapping("/usuarios")
    public String gerenciarUsuarios(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("listaUsuarios", usuarioRepository.findAll());
        return "admin/gerenciar-usuarios";
    }

    @GetMapping("/usuarios/novo")
    public String mostrarFormularioNovoUsuario(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("usuario", new Usuario());
        return "admin/formulario-usuario";
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute("usuario") Usuario usuarioDoFormulario,
                                @RequestParam(value = "profilePictureFile", required = false) MultipartFile profilePictureFile,
                                RedirectAttributes redirectAttributes) {

        Optional<Usuario> outroUsuarioComEmail = usuarioRepository.findByEmail(usuarioDoFormulario.getEmail());
        if (outroUsuarioComEmail.isPresent() && outroUsuarioComEmail.get().getId() != usuarioDoFormulario.getId()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro: O e-mail '" + usuarioDoFormulario.getEmail() + "' já está em uso por outro usuário.");
            return "redirect:/admin/usuarios/novo";
        }

        if (usuarioDoFormulario.getId() != 0) {
            Usuario usuarioDoBanco = usuarioRepository.findById(usuarioDoFormulario.getId()).orElseThrow(() -> new IllegalArgumentException("ID de usuário inválido:" + usuarioDoFormulario.getId()));
            usuarioDoBanco.setNome(usuarioDoFormulario.getNome());
            usuarioDoBanco.setEmail(usuarioDoFormulario.getEmail());
            usuarioDoBanco.setTipoPerfil(usuarioDoFormulario.getTipoPerfil());

            if (usuarioDoFormulario.getSenha() != null && !usuarioDoFormulario.getSenha().isEmpty()) {
                usuarioDoBanco.setSenha(passwordEncoder.encode(usuarioDoFormulario.getSenha()));
            }

            if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
                String nomeDoFicheiro = fileStorageService.storeFile(profilePictureFile);
                usuarioDoBanco.setProfilePictureUrl("/uploads/" + nomeDoFicheiro);
            } else if (usuarioDoFormulario.getProfilePictureUrl() != null && usuarioDoFormulario.getProfilePictureUrl().isEmpty()) {
                usuarioDoBanco.setProfilePictureUrl(null);
            }

            usuarioRepository.save(usuarioDoBanco);
        } else {
            if (usuarioDoFormulario.getSenha() == null || usuarioDoFormulario.getSenha().isEmpty()) {
                redirectAttributes.addFlashAttribute("mensagemErro", "Erro: A senha é obrigatória para novos usuários.");
                return "redirect:/admin/usuarios/novo";
            }
            usuarioDoFormulario.setSenha(passwordEncoder.encode(usuarioDoFormulario.getSenha()));

            if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
                String nomeDoFicheiro = fileStorageService.storeFile(profilePictureFile);
                usuarioDoFormulario.setProfilePictureUrl("/uploads/" + nomeDoFicheiro);
            }

            usuarioRepository.save(usuarioDoFormulario);
        }

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuário salvo com sucesso!");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditarUsuario(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("ID de usuário inválido:" + id));
        usuario.setSenha("");
        model.addAttribute("usuario", usuario);
        return "admin/formulario-usuario";
    }

    @GetMapping("/usuarios/excluir/{id}")
    public String excluirUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        List<SessaoTeste> sessoes = sessaoTesteRepository.findByTestadorId(id);
        if (!sessoes.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível excluir o usuário, pois ele possui sessões de teste associadas.");
            return "redirect:/admin/usuarios";
        }

        usuarioRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Usuário excluído com sucesso!");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/projetos")
    public String gerenciarProjetos(Model model, @RequestParam(defaultValue = "nome") String sortField, @RequestParam(defaultValue = "asc") String sortDir, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortField);

        model.addAttribute("listaProjetos", projetoRepository.findAll(sort));
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "admin/gerenciar-projetos";
    }

    @PostMapping("/projetos/salvar")
    public String salvarProjeto(@ModelAttribute("projeto") Projeto projetoDoFormulario, RedirectAttributes redirectAttributes) {
        if (projetoDoFormulario.getId() != 0) {
            Projeto projetoDoBanco = projetoRepository.findById(projetoDoFormulario.getId()).orElseThrow(() -> new IllegalArgumentException("ID de projeto inválido:" + projetoDoFormulario.getId()));
            projetoDoBanco.setNome(projetoDoFormulario.getNome());
            projetoDoBanco.setDescricao(projetoDoFormulario.getDescricao());
            projetoRepository.save(projetoDoBanco);
        } else {
            projetoRepository.save(projetoDoFormulario);
        }

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Projeto salvo com sucesso!");
        return "redirect:/admin/projetos";
    }

    @GetMapping("/projetos/novo")
    public String mostrarFormularioNovoProjeto(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("projeto", new Projeto());
        return "admin/formulario-projeto";
    }

    @GetMapping("/projetos/editar/{id}")
    public String mostrarFormularioEditarProjeto(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        Projeto projeto = projetoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("ID de projeto inválido:" + id));
        model.addAttribute("projeto", projeto);
        return "admin/formulario-projeto";
    }

    @GetMapping("/projetos/excluir/{id}")
    public String excluirProjeto(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        if (!sessaoTesteRepository.findByProjetoId(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível excluir o projeto, pois ele possui sessões de teste associadas.");
            return "redirect:/admin/projetos";
        }

        projetoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Projeto excluído com sucesso!");
        return "redirect:/admin/projetos";
    }

    @GetMapping("/projetos/{projetoId}/membros")
    public String gerenciarMembros(@PathVariable("projetoId") Integer projetoId, Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de projeto inválido:" + projetoId));

        List<Usuario> membrosAtuais = List.copyOf(projeto.getMembros());
        List<Usuario> todosUsuarios = usuarioRepository.findAll();

        List<Integer> idsMembrosAtuais = membrosAtuais.stream().map(Usuario::getId).collect(Collectors.toList());

        List<Usuario> usuariosDisponiveis = todosUsuarios.stream()
                .filter(u -> !idsMembrosAtuais.contains(u.getId()))
                .collect(Collectors.toList());

        model.addAttribute("projeto", projeto);
        model.addAttribute("membrosAtuais", membrosAtuais);
        model.addAttribute("usuariosDisponiveis", usuariosDisponiveis);

        return "admin/gerenciar-membros";
    }

    @PostMapping("/projetos/adicionarMembro")
    public String adicionarMembro(@RequestParam("projetoId") Integer projetoId, @RequestParam("usuarioId") Integer usuarioId, RedirectAttributes redirectAttributes) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de projeto inválido:" + projetoId));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID de usuário inválido:" + usuarioId));

        projeto.getMembros().add(usuario);
        projetoRepository.save(projeto);

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Membro adicionado com sucesso!");
        return "redirect:/admin/projetos/" + projetoId + "/membros";
    }

    @PostMapping("/projetos/removerMembro")
    public String removerMembro(@RequestParam("projetoId") Integer projetoId, @RequestParam("usuarioId") Integer usuarioId, RedirectAttributes redirectAttributes) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de projeto inválido:" + projetoId));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID de usuário inválido:" + usuarioId));

        projeto.getMembros().remove(usuario);
        projetoRepository.save(projeto);

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Membro removido com sucesso!");
        return "redirect:/admin/projetos/" + projetoId + "/membros";
    }

    @GetMapping("/estrategias")
    public String gerenciarEstrategias(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("listaEstrategias", estrategiaRepository.findAll());
        return "admin/gerenciar-estrategias";
    }

    @GetMapping("/estrategias/nova")
    public String mostrarFormularioNovaEstrategia(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("estrategia", new Estrategia());
        return "admin/formulario-estrategia";
    }

    @PostMapping("/estrategias/salvar")
    public String salvarEstrategia(@ModelAttribute("estrategia") Estrategia estrategiaDoFormulario, @RequestParam("imagemFile") MultipartFile imagemFile, RedirectAttributes redirectAttributes) {

        if (!imagemFile.isEmpty()) {
            String nomeDoFicheiro = fileStorageService.storeFile(imagemFile);
            estrategiaDoFormulario.setImagemUrl("/uploads/" + nomeDoFicheiro);
        }

        if (estrategiaDoFormulario.getId() != 0) {
            Estrategia estrategiaDoBanco = estrategiaRepository.findById(estrategiaDoFormulario.getId()).orElseThrow(() -> new IllegalArgumentException("ID de estratégia inválido:" + estrategiaDoFormulario.getId()));
            estrategiaDoBanco.setNome(estrategiaDoFormulario.getNome());
            estrategiaDoBanco.setDescricao(estrategiaDoFormulario.getDescricao());
            estrategiaDoBanco.setExemplos(estrategiaDoFormulario.getExemplos());
            estrategiaDoBanco.setDicas(estrategiaDoFormulario.getDicas());
            if (estrategiaDoFormulario.getImagemUrl() != null) {
                estrategiaDoBanco.setImagemUrl(estrategiaDoFormulario.getImagemUrl());
            }
            estrategiaRepository.save(estrategiaDoBanco);
        } else {
            estrategiaRepository.save(estrategiaDoFormulario);
        }
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Estratégia salva com sucesso!");
        return "redirect:/admin/estrategias";
    }

    @GetMapping("/estrategias/editar/{id}")
    public String mostrarFormularioEditarEstrategia(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        Estrategia estrategia = estrategiaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("ID de estratégia inválido:" + id));
        model.addAttribute("estrategia", estrategia);
        return "admin/formulario-estrategia";
    }

    @GetMapping("/estrategias/excluir/{id}")
    public String excluirEstrategia(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        Estrategia estrategia = estrategiaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("ID de estratégia inválido:" + id));
        if (estrategia.getSessoesDeTeste() != null && !estrategia.getSessoesDeTeste().isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível excluir a estratégia, pois ela está em uso em sessões de teste.");
            return "redirect:/admin/estrategias";
        }

        estrategiaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Estratégia excluída com sucesso!");
        return "redirect:/admin/estrategias";
    }

    @GetMapping("/projetos/{projetoId}/sessoes")
    public String gerenciarSessoesDoProjeto(@PathVariable("projetoId") Integer projetoId, Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de projeto inválido:" + projetoId));

        model.addAttribute("projeto", projeto);
        model.addAttribute("listaSessoes", sessaoTesteRepository.findByProjetoId(projetoId));

        return "admin/gerenciar-sessoes";
    }

    @GetMapping("/sessoes/{sessaoId}/bugs")
    public String visualizarBugsDaSessao(@PathVariable("sessaoId") Integer sessaoId, Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("listaBugs", bugRepository.findBySessaoTesteId(sessaoId));
        model.addAttribute("sessao", sessaoTesteRepository.findById(sessaoId).orElse(null));
        return "admin/lista-bugs-admin";
    }

    @GetMapping("/sessoes")
    public String gerenciarTodasSessoes(Model model, Authentication authentication) {
        addAuthenticatedUserToModel(model, authentication);
        model.addAttribute("listaSessoes", sessaoTesteRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHoraCriacao")));
        return "admin/gerenciar-todas-sessoes";
    }

    @GetMapping("/sessoes/excluir/{id}")
    public String excluirSessao(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            sessaoTesteRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Sessão de teste excluída com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não foi possível excluir a sessão de teste.");
        }
        return "redirect:/admin/sessoes";
    }
}