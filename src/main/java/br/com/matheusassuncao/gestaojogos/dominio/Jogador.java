package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Perfil de associado vinculado a um usuário. Só existe após a aprovação
 * de um administrador (UC27).
 */
@Entity
@Table(name = "jogador")
public class Jogador {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "matricula_associado", length = 50, unique = true)
    private String matriculaAssociado;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_associativa", nullable = false, length = 20)
    private SituacaoAssociativa situacaoAssociativa = SituacaoAssociativa.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "aprovado_em")
    private OffsetDateTime aprovadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private Usuario aprovadoPor;

    protected Jogador() {
        // exigido pelo JPA
    }

    public Jogador(Usuario usuario,
                   String matriculaAssociado,
                   Categoria categoria,
                   SituacaoAssociativa situacaoAssociativa,
                   Usuario aprovadoPor) {
        this.usuario = usuario;
        this.matriculaAssociado = matriculaAssociado;
        this.categoria = categoria;
        this.situacaoAssociativa = situacaoAssociativa;
        this.aprovadoPor = aprovadoPor;
        this.aprovadoEm = OffsetDateTime.now();
    }

    /**
     * RN01: só pode se inscrever em partidas quem está com a situação
     * associativa regular.
     */
    public boolean estaRegular() {
        return situacaoAssociativa == SituacaoAssociativa.REGULAR;
    }

    public void alterarSituacao(SituacaoAssociativa situacaoAssociativa) {
        this.situacaoAssociativa = situacaoAssociativa;
    }

    public void alterarCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getMatriculaAssociado() {
        return matriculaAssociado;
    }

    public SituacaoAssociativa getSituacaoAssociativa() {
        return situacaoAssociativa;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public OffsetDateTime getAprovadoEm() {
        return aprovadoEm;
    }

    public Usuario getAprovadoPor() {
        return aprovadoPor;
    }
}
