package com.gametester.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nome;

    @Column(unique = true, nullable = false)
    @Email(message = "O e-mail deve ter um formato v\u00e1lido.")
    private String email;

    @Column(nullable = false)
    @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres.") // Valida\u00e7\u00e3o de tamanho
    private String senha;

    @Column(nullable = false)
    private String tipoPerfil;

    private String profilePictureUrl;

    @OneToMany(mappedBy = "testador", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SessaoTeste> sessoesDeTeste;

    @ManyToMany(mappedBy = "membros")
    @JsonIgnore
    private Set<Projeto> projetos;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTipoPerfil() {
        return tipoPerfil;
    }

    public void setTipoPerfil(String tipoPerfil) {
        this.tipoPerfil = tipoPerfil;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Set<SessaoTeste> getSessoesDeTeste() {
        return sessoesDeTeste;
    }

    public void setSessoesDeTeste(Set<SessaoTeste> sessoesDeTeste) {
        this.sessoesDeTeste = sessoesDeTeste;
    }

    public Set<Projeto> getProjetos() {
        return projetos;
    }

    public void setProjetos(Set<Projeto> projetos) {
        this.projetos = projetos;
    }
}