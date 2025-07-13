package com.gametester.controller.api;

import com.gametester.model.Usuario;
import com.gametester.repository.SessaoTesteRepository;
import com.gametester.repository.UsuarioRepository;
import com.gametester.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final SessaoTesteRepository sessaoTesteRepository;

    public UsuarioApiController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, FileStorageService fileStorageService, SessaoTesteRepository sessaoTesteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.sessaoTesteRepository = sessaoTesteRepository;
    }

    private void validateTipoPerfil(String tipoPerfil) {
        Set<String> tiposPermitidos = Set.of("ADMINISTRADOR", "TESTADOR");
        if (!tiposPermitidos.contains(tipoPerfil)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O perfil de usu\u00e1rio fornecido \u00e9 inv\u00e1lido. Perfis permitidos: ADMINISTRADOR, TESTADOR.");
        }
    }

    @GetMapping
    public List<Usuario> getAllUsers() {
        return usuarioRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> getUserById(@PathVariable int id) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);
        return usuario.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario createUser(@Valid @RequestBody Usuario usuario) {
        validateTipoPerfil(usuario.getTipoPerfil());

        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail j\u00e1 em uso.");
        }
        if (usuario.getSenha() == null || usuario.getSenha().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha \u00e9 obrigat\u00f3ria.");
        }
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> updateUser(@PathVariable int id, @Valid @RequestBody Usuario usuarioDetails) {
        validateTipoPerfil(usuarioDetails.getTipoPerfil());

        return usuarioRepository.findById(id)
                .map(usuario -> {
                    Optional<Usuario> outroUsuarioComEmail = usuarioRepository.findByEmail(usuarioDetails.getEmail());
                    if (outroUsuarioComEmail.isPresent() && outroUsuarioComEmail.get().getId() != id) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail j\u00e1 em uso por outro usu\u00e1rio.");
                    }

                    usuario.setNome(usuarioDetails.getNome());
                    usuario.setEmail(usuarioDetails.getEmail());
                    usuario.setTipoPerfil(usuarioDetails.getTipoPerfil());

                    if (usuarioDetails.getSenha() != null && !usuarioDetails.getSenha().isEmpty()) {
                        usuario.setSenha(passwordEncoder.encode(usuarioDetails.getSenha()));
                    }

                    if (usuarioDetails.getProfilePictureUrl() != null) {
                        usuario.setProfilePictureUrl(usuarioDetails.getProfilePictureUrl());
                    }

                    return ResponseEntity.ok(usuarioRepository.save(usuario));
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable int id) {
        if (usuarioRepository.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usu\u00e1rio n\u00e3o encontrado.");
        }
        if (!sessaoTesteRepository.findByTestadorId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "N\u00e3o \u00e9 poss\u00edvel excluir o usu\u00e1rio, pois ele possui sess\u00f5es de teste associadas.");
        }
        usuarioRepository.deleteById(id);
    }

    @PostMapping("/{id}/upload-profile-picture")
    public ResponseEntity<Usuario> uploadProfilePicture(@PathVariable int id, @RequestParam("file") MultipartFile file) {
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    if (file.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo da foto de perfil n\u00e3o pode estar vazio.");
                    }
                    String nomeDoFicheiro = fileStorageService.storeFile(file);
                    usuario.setProfilePictureUrl("/uploads/" + nomeDoFicheiro);
                    return ResponseEntity.ok(usuarioRepository.save(usuario));
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}