package com.gametester.controller.api;

import com.gametester.model.Estrategia;
import com.gametester.repository.EstrategiaRepository;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/estrategias")
public class EstrategiaApiController {

    private final EstrategiaRepository estrategiaRepository;
    private final SessaoTesteRepository sessaoTesteRepository;
    private final FileStorageService fileStorageService;

    public EstrategiaApiController(EstrategiaRepository estrategiaRepository, SessaoTesteRepository sessaoTesteRepository, FileStorageService fileStorageService) {
        this.estrategiaRepository = estrategiaRepository;
        this.sessaoTesteRepository = sessaoTesteRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<Estrategia> getAllEstrategias() {
        return estrategiaRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Estrategia> getEstrategiaById(@PathVariable int id) {
        Optional<Estrategia> estrategia = estrategiaRepository.findById(id);
        return estrategia.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Estrategia createEstrategia(@RequestPart("estrategia") Estrategia estrategia,
                                       @RequestPart(value = "imagemFile", required = false) MultipartFile imagemFile) {
        if (imagemFile != null && !imagemFile.isEmpty()) {
            String nomeDoFicheiro = fileStorageService.storeFile(imagemFile);
            estrategia.setImagemUrl("/uploads/" + nomeDoFicheiro);
        }
        return estrategiaRepository.save(estrategia);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Estrategia> updateEstrategia(@PathVariable int id,
                                                       @RequestPart("estrategia") Estrategia estrategiaDetails,
                                                       @RequestPart(value = "imagemFile", required = false) MultipartFile imagemFile) {
        return estrategiaRepository.findById(id)
                .map(estrategia -> {
                    estrategia.setNome(estrategiaDetails.getNome());
                    estrategia.setDescricao(estrategiaDetails.getDescricao());
                    estrategia.setExemplos(estrategiaDetails.getExemplos());
                    estrategia.setDicas(estrategiaDetails.getDicas());

                    if (imagemFile != null && !imagemFile.isEmpty()) {
                        String nomeDoFicheiro = fileStorageService.storeFile(imagemFile);
                        estrategia.setImagemUrl("/uploads/" + nomeDoFicheiro);
                    } else if (estrategiaDetails.getImagemUrl() != null && estrategiaDetails.getImagemUrl().isEmpty()) {
                        estrategia.setImagemUrl(null);
                    }

                    return ResponseEntity.ok(estrategiaRepository.save(estrategia));
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEstrategia(@PathVariable int id) {
        if (estrategiaRepository.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estratégia não encontrada.");
        }
        if (sessaoTesteRepository.findByEstrategiaId(id) != null && !sessaoTesteRepository.findByEstrategiaId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível excluir a estratégia, pois ela está em uso em sessões de teste.");
        }
        estrategiaRepository.deleteById(id);
    }
}