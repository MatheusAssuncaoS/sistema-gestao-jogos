package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "token_recuperacao_senha")
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 255, unique = true)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(name = "usado_em")
    private OffsetDateTime usadoEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected TokenRecuperacaoSenha() {
        // exigido pelo JPA
    }

    public TokenRecuperacaoSenha(Usuario usuario, String token, OffsetDateTime expiraEm) {
        this.usuario = usuario;
        this.token = token;
        this.expiraEm = expiraEm;
    }

    /**
     * Um token só pode ser usado uma vez e antes de expirar.
     */
    public boolean estaValido() {
        return usadoEm == null && expiraEm.isAfter(OffsetDateTime.now());
    }

    public void marcarComoUsado() {
        this.usadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getExpiraEm() {
        return expiraEm;
    }

    public OffsetDateTime getUsadoEm() {
        return usadoEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
