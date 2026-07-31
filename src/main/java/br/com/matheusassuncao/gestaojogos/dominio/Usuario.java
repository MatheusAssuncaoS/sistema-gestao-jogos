package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUsuario status = StatusUsuario.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_papel",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "papel_id")
    )
    private Set<Papel> papeis = new HashSet<>();

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public Set<Papel> getPapeis() {
        return papeis;
    }

    public String getNome() {
        return nome;
    }

    public UUID getId() {
        return id;
    }

    /**
     * A regra de "o que pode mudar" pertence à entidade: nome e e-mail sim,
     * senha e status não passam por aqui.
     */
    public void alterarDados(String nome, String email) {
        this.nome = nome;
        this.email = email;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void alterarSenha(String senhaHash) {
        this.senhaHash = senhaHash;
        this.atualizadoEm = OffsetDateTime.now();
    }
}