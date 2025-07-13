package com.gametester.controller.api;

import com.gametester.model.Bug;
import com.gametester.model.SessaoTeste;
import com.gametester.model.Usuario;
import com.gametester.repository.BugRepository;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.repository.UsuarioRepository;
import com.gametester.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bugs")
public class BugApiController {

    private final BugRepository bugRepository;
    private final SessaoTesteRepository sessaoTesteRepository;
    private final UsuarioRepository usuarioRepository;
    private final FileStorageService fileStorageService;

    public BugApiController(BugRepository bugRepository, SessaoTesteRepository sessaoTesteRepository, UsuarioRepository usuarioRepository, FileStorageService fileStorageService) {
        this.bugRepository = bugRepository;
        this.sessaoTesteRepository = sessaoTesteRepository;
        this.usuarioRepository = usuarioRepository;
        this.fileStorageService = fileStorageService;
    }

    private Usuario getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usu\u00e1rio n\u00e3o autenticado.");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usu\u00e1rio autenticado n\u00e3o encontrado no sistema."));
    }

    @GetMapping
    public List<Bug> getAllBugs(Authentication authentication) {
        Usuario usuarioLogado = getAuthenticatedUser(authentication);
        if (usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR")) {
            return bugRepository.findAll();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para listar todos os bugs.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bug> getBugById(@PathVariable int id, Authentication authentication) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bug n\u00e3o encontrado."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") || bug.getSessaoTeste().getTestador().getId() == usuarioLogado.getId()) {
            return ResponseEntity.ok(bug);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voc\u00ea n\u00e3o tem permiss\u00e3o para ver este bug.");
    }

    @GetMapping("/por-sessao/{sessaoId}")
    public List<Bug> getBugsBySessaoTesteId(@PathVariable int sessaoId, Authentication authentication) {
        SessaoTeste sessao = sessaoTesteRepository.findById(sessaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sess\u00e3o n\u00e3o encontrada."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") || sessao.getTestador().getId() == usuarioLogado.getId()) {
            return bugRepository.findBySessaoTesteId(sessaoId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voc\u00ea n\u00e3o tem permiss\u00e3o para ver os bugs desta sess\u00e3o.");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Bug createBug(@RequestBody Bug bug, Authentication authentication) {
        SessaoTeste sessao = sessaoTesteRepository.findById(bug.getSessaoTeste().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sess\u00e3o de teste inv\u00e1lida."));

        Usuario testadorLogado = getAuthenticatedUser(authentication);

        if (!testadorLogado.getTipoPerfil().equals("ADMINISTRADOR") && sessao.getTestador().getId() != testadorLogado.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voc\u00ea n\u00e3o tem permiss\u00e3o para registrar bug nesta sess\u00e3o.");
        }

        if (!"EM_EXECUCAO".equals(sessao.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "N\u00e3o \u00e9 poss\u00edvel registrar bugs em uma sess\u00e3o que n\u00e3o est\u00e1 'Em Execu\u00e7\u00e3o'.");
        }

        bug.setSessaoTeste(sessao);

        return bugRepository.save(bug);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bug> updateBug(@PathVariable int id, @RequestBody Bug bugDetails, Authentication authentication) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bug n\u00e3o encontrado."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (!usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") && bug.getSessaoTeste().getTestador().getId() != usuarioLogado.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voc\u00ea n\u00e3o tem permiss\u00e3o para atualizar este bug.");
        }

        if ("FINALIZADO".equals(bug.getSessaoTeste().getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "N\u00e3o \u00e9 poss\u00edvel atualizar um bug em uma sess\u00e3o finalizada.");
        }

        bug.setDescricao(bugDetails.getDescricao());
        bug.setSeveridade(bugDetails.getSeveridade());
        bug.setScreenshotUrl(bugDetails.getScreenshotUrl());

        return ResponseEntity.ok(bugRepository.save(bug));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBug(@PathVariable int id, Authentication authentication) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bug n\u00e3o encontrado."));

        Usuario usuarioLogado = getAuthenticatedUser(authentication);

        if (!usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voc\u00ea n\u00e3o tem permiss\u00e3o para excluir este bug.");
        }

        bugRepository.deleteById(id);
    }

    @PostMapping("/{id}/upload-screenshot")
    public ResponseEntity<Bug> uploadBugScreenshot(@PathVariable int id, @RequestParam("file") MultipartFile file, Authentication authentication) {
        return bugRepository.findById(id)
                .map(bug -> {
                    Usuario usuarioLogado = getAuthenticatedUser(authentication);
                    if (!usuarioLogado.getTipoPerfil().equals("ADMINISTRADOR") && bug.getSessaoTeste().getTestador().getId() != usuarioLogado.getId()) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voc\u00ea n\u00e3o tem permiss\u00e3o para fazer upload de screenshot para este bug.");
                    }
                    if (file.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo de screenshot n\u00e3o pode estar vazio.");
                    }
                    if ("FINALIZADO".equals(bug.getSessaoTeste().getStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "N\u00e3o \u00e9 poss\u00edvel fazer upload de screenshot para um bug em sess\u00e3o finalizada.");
                    }

                    String nomeDoFicheiro = fileStorageService.storeFile(file);
                    bug.setScreenshotUrl("/uploads/" + nomeDoFicheiro);
                    return ResponseEntity.ok(bugRepository.save(bug));
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}