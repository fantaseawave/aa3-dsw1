package com.gametester.controller.api;

import com.gametester.model.SessaoTeste;
import com.gametester.model.Usuario;
import com.gametester.model.Projeto;
import com.gametester.model.Estrategia;
import com.gametester.repository.EstrategiaRepository;
import com.gametester.repository.ProjetoRepository;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessoes")
public class SessaoTesteApiController {

    private final SessaoTesteRepository sessaoTesteRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstrategiaRepository estrategiaRepository;

    public SessaoTesteApiController(SessaoTesteRepository sessaoTesteRepository, ProjetoRepository projetoRepository, UsuarioRepository usuarioRepository, EstrategiaRepository estrategiaRepository) {
        this.sessaoTesteRepository = sessaoTesteRepository;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estrategiaRepository = estrategiaRepository;
    }

    private Usuario getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário autenticado não encontrado no sistema."));
    }

    @GetMapping
    public List<SessaoTeste> getAllSessoes(Authentication authentication) {
        Usuario usuarioLogado = getAuthenticatedUser(authentication);
        if (usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR")) {
            return sessaoTesteRepository.findAll();
        } else if (usuarioLogado.getTipoPerfil().equals("TESTADOR")) {
            return sessaoTesteRepository.findByTestadorId(usuarioLogado.getId());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para listar sessões.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessaoTeste> getSessaoTesteById(@PathVariable int id, Authentication authentication) {
        SessaoTeste sessao = sessaoTesteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") || sessao.getTestador().getId() == usuarioLogado.getId()) {
            return ResponseEntity.ok(sessao);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para ver esta sessão.");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoTeste createSessao(@RequestBody SessaoTeste sessaoTeste, Authentication authentication) {
        if (sessaoTeste.getTempoSessaoMinutos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tempo da sessão deve ser maior que 0 minutos.");
        }

        Usuario testadorLogado = getAuthenticatedUser(authentication);

        if (testadorLogado.getTipoPerfil().equals("TESTADOR") && sessaoTeste.getTestador() != null && sessaoTeste.getTestador().getId() != testadorLogado.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Testadores só podem criar sessões para si mesmos.");
        }
        if (testadorLogado.getTipoPerfil().equals("ADMINISTRADOR") && sessaoTeste.getTestador() != null) {
            Usuario testadorAtribuido = usuarioRepository.findById(sessaoTeste.getTestador().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de testador inválido."));
            sessaoTeste.setTestador(testadorAtribuido);
        } else {
            sessaoTeste.setTestador(testadorLogado);
        }


        Projeto projeto = projetoRepository.findById(sessaoTeste.getProjeto().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Projeto não encontrado."));
        Estrategia estrategia = estrategiaRepository.findById(sessaoTeste.getEstrategia().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estratégia não encontrada."));

        if (testadorLogado.getTipoPerfil().equals("TESTADOR") && !projeto.getMembros().contains(testadorLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é membro deste projeto.");
        }
        // LINHA CORRIGIDA AQUI: agora passa o objeto Usuario completo para contains()
        if (testadorLogado.getTipoPerfil().equals("ADMINISTRADOR") && sessaoTeste.getTestador() != null && !projeto.getMembros().contains(sessaoTeste.getTestador())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O testador atribuído não é membro deste projeto.");
        }


        sessaoTeste.setProjeto(projeto);
        sessaoTeste.setEstrategia(estrategia);
        sessaoTeste.setStatus("CRIADO");

        return sessaoTesteRepository.save(sessaoTeste);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessaoTeste> updateSessao(@PathVariable int id, @RequestBody SessaoTeste sessaoDetails, Authentication authentication) {
        SessaoTeste sessao = sessaoTesteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (!usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") && sessao.getTestador().getId() != usuarioLogado.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para atualizar esta sessão.");
        }

        if ("FINALIZADO".equals(sessao.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível atualizar uma sessão finalizada.");
        }
        if (sessaoDetails.getTempoSessaoMinutos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tempo da sessão deve ser maior que 0 minutos.");
        }

        sessao.setDescricao(sessaoDetails.getDescricao());
        sessao.setTempoSessaoMinutos(sessaoDetails.getTempoSessaoMinutos());

        return ResponseEntity.ok(sessaoTesteRepository.save(sessao));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSessao(@PathVariable int id, Authentication authentication) {
        SessaoTeste sessao = sessaoTesteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (!usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para excluir esta sessão.");
        }

        sessaoTesteRepository.deleteById(id);
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<SessaoTeste> iniciarSessao(@PathVariable int id, Authentication authentication) {
        SessaoTeste sessao = sessaoTesteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (!usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") && sessao.getTestador().getId() != usuarioLogado.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para iniciar esta sessão.");
        }

        if (!"CRIADO".equals(sessao.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A sessão já foi iniciada ou finalizada.");
        }

        sessao.setStatus("EM_EXECUCAO");
        sessao.setDataHoraInicio(new Timestamp(System.currentTimeMillis()));
        return ResponseEntity.ok(sessaoTesteRepository.save(sessao));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<SessaoTeste> finalizarSessao(@PathVariable int id, Authentication authentication) {
        SessaoTeste sessao = sessaoTesteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (!usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") && sessao.getTestador().getId() != usuarioLogado.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para finalizar esta sessão.");
        }

        if ("FINALIZADO".equals(sessao.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A sessão já está finalizada.");
        }
        if (!"EM_EXECUCAO".equals(sessao.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A sessão não está em execução para ser finalizada.");
        }

        sessao.setStatus("FINALIZADO");
        sessao.setDataHoraFim(new Timestamp(System.currentTimeMillis()));
        return ResponseEntity.ok(sessaoTesteRepository.save(sessao));
    }

    @GetMapping("/por-projeto/{projetoId}")
    public List<SessaoTeste> getSessoesByProjeto(@PathVariable int projetoId, Authentication authentication) {
        Usuario usuarioLogado = getAuthenticatedUser(authentication);
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado."));

        if (usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") || projeto.getMembros().contains(usuarioLogado)) {
            return sessaoTesteRepository.findByProjetoId(projetoId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para ver sessões deste projeto.");
    }

    @GetMapping("/por-testador/{testadorId}")
    public List<SessaoTeste> getSessoesByTestador(@PathVariable int testadorId, Authentication authentication) {
        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") || usuarioLogado.getId() == testadorId) {
            return sessaoTesteRepository.findByTestadorId(testadorId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para ver as sessões deste testador.");
    }
}