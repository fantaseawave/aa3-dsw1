package com.gametester.controller.api;

import com.gametester.model.Projeto;
import com.gametester.model.Usuario;
import com.gametester.repository.ProjetoRepository;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoApiController {

    private final ProjetoRepository projetoRepository;
    private final SessaoTesteRepository sessaoTesteRepository;
    private final UsuarioRepository usuarioRepository;

    public ProjetoApiController(ProjetoRepository projetoRepository, SessaoTesteRepository sessaoTesteRepository, UsuarioRepository usuarioRepository) {
        this.projetoRepository = projetoRepository;
        this.sessaoTesteRepository = sessaoTesteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Projeto> getAllProjetos(@RequestParam(defaultValue = "nome") String sortField, @RequestParam(defaultValue = "asc") String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortField);
        return projetoRepository.findAll(sort);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Projeto> getProjetoById(@PathVariable int id) {
        Optional<Projeto> projeto = projetoRepository.findById(id);
        return projeto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Projeto createProjeto(@RequestBody Projeto projeto) {
        return projetoRepository.save(projeto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Projeto> updateProjeto(@PathVariable int id, @RequestBody Projeto projetoDetails) {
        return projetoRepository.findById(id)
                .map(projeto -> {
                    projeto.setNome(projetoDetails.getNome());
                    projeto.setDescricao(projetoDetails.getDescricao());
                    return ResponseEntity.ok(projetoRepository.save(projeto));
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjeto(@PathVariable int id) {
        if (projetoRepository.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto n\u00e3o encontrado.");
        }
        if (!sessaoTesteRepository.findByProjetoId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "N\u00e3o \u00e9 poss\u00edvel excluir o projeto, pois ele possui sess\u00f5es de teste associadas.");
        }
        projetoRepository.deleteById(id);
    }


    @GetMapping("/{id}/membros")
    public Set<Usuario> getMembrosDoProjeto(@PathVariable int id) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto n\u00e3o encontrado."));
        return projeto.getMembros();
    }

    @PostMapping("/{id}/membros")
    @ResponseStatus(HttpStatus.OK)
    public Projeto adicionarMembro(@PathVariable int id, @RequestBody Usuario membroDetails) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto n\u00e3o encontrado."));
        Usuario membro = usuarioRepository.findById(membroDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usu\u00e1rio membro n\u00e3o encontrado."));

        if (projeto.getMembros().contains(membro)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usu\u00e1rio j\u00e1 \u00e9 membro deste projeto.");
        }

        projeto.getMembros().add(membro);
        return projetoRepository.save(projeto);
    }

    @DeleteMapping("/{id}/membros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerMembro(@PathVariable int id, @PathVariable int usuarioId) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto n\u00e3o encontrado."));
        Usuario membro = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usu\u00e1rio membro n\u00e3o encontrado."));

        if (!projeto.getMembros().contains(membro)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usu\u00e1rio n\u00e3o \u00e9 membro deste projeto.");
        }

        projeto.getMembros().remove(membro);
        projetoRepository.save(projeto);
    }
}