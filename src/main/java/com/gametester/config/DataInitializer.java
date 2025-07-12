package com.gametester.config;

import com.gametester.model.Usuario;
import com.gametester.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.findByEmail("admin@email.com").isEmpty()) {

            Usuario admin = new Usuario();
            admin.setNome("Administrador Padrão");
            admin.setEmail("admin@email.com");
            admin.setSenha(passwordEncoder.encode("admin"));
            admin.setTipoPerfil("ADMINISTRADOR");

            usuarioRepository.save(admin);
        }
    }
}