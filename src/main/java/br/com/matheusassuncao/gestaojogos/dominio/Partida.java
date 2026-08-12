package br.com.matheusassuncao.gestaojogos.dominio;

import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Partida de uma modalidade, em um local e horário.
 *
 * O campo `versao` implementa lock otimista: a edição pelo organizador é uma
 * operação longa (formulário aberto) e de baixa contenção, então não faz
 * sentido segurar um lock de banco. Se duas edições colidirem, a segunda
 * recebe um erro e recarrega. O lock pessimista fica reservado para o fluxo
 * de inscrição (issue #6), curto e disputado.
 */
@Entity
@Table(name = "partida")
public class Partida {

    private static final int CAPACIDADE_EQUIPE_FUTEBOL = 8;

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modalidade_id", nullable = false)
    private Modalidade modalidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "local_id", nullable = false)
    private LocalPartida local;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private Integer capacidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPartida status = StatusPartida.RASCUNHO;

    @Column(name = "inscricoes_abrem_em")
    private OffsetDateTime inscricoesAbremEm;

    @Column(name = "inscricoes_encerram_em")
    private OffsetDateTime inscricoesEncerramEm;

    @Column(name = "escala_publicada", nullable = false)
    private Boolean escalaPublicada = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por", nullable = false)
    private Usuario criadoPor;

    @Version
    @Column(nullable = false)
    private Integer versao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Equipe> equipes = new ArrayList<>();

    protected Partida() {
        // exigido pelo JPA
    }

    public Partida(Modalidade modalidade,
                   LocalPartida local,
                   Categoria categoria,
                   OffsetDateTime inicio,
                   Integer capacidade,
                   OffsetDateTime inscricoesAbremEm,
                   OffsetDateTime inscricoesEncerramEm,
                   Usuario criadoPor) {

        this.modalidade = modalidade;
        this.local = local;
        this.categoria = categoria;
        this.inicio = inicio;
        this.capacidade = capacidade;
        this.inscricoesAbremEm = inscricoesAbremEm;
        this.inscricoesEncerramEm = inscricoesEncerramEm;
        this.criadoPor = criadoPor;

        criarEquipesDoFutebol();
    }

    /**
     * As duas equipes nascem com a partida. A cor informada ao jogador
     * depende da publicação da escalação (RN05).
     */
    private void criarEquipesDoFutebol() {
        equipes.add(new Equipe(this, "Equipe Azul", Equipe.COR_AZUL, CAPACIDADE_EQUIPE_FUTEBOL));
        equipes.add(new Equipe(this, "Equipe Amarela", Equipe.COR_AMARELO, CAPACIDADE_EQUIPE_FUTEBOL));
    }

    public void alterarDados(LocalPartida local,
                             Categoria categoria,
                             OffsetDateTime inicio,
                             OffsetDateTime inscricoesAbremEm,
                             OffsetDateTime inscricoesEncerramEm) {

        if (!status.aceitaEdicao()) {
            throw new RegraNegocioException(
                    "Só é possível editar partidas em rascunho ou abertas."
            );
        }

        this.local = local;
        this.categoria = categoria;
        this.inicio = inicio;
        this.inscricoesAbremEm = inscricoesAbremEm;
        this.inscricoesEncerramEm = inscricoesEncerramEm;
    }

    public void abrir() {
        if (status != StatusPartida.RASCUNHO) {
            throw new RegraNegocioException(
                    "Só é possível abrir partidas que estão em rascunho."
            );
        }

        this.status = StatusPartida.ABERTA;
    }

    public void cancelar() {
        if (status.estaEncerrada()) {
            throw new RegraNegocioException(
                    "Esta partida já foi encerrada, finalizada ou cancelada."
            );
        }

        this.status = StatusPartida.CANCELADA;
    }

    /**
     * RN04: ao atingir a capacidade, a partida deixa de aceitar inscrições
     * diretas. O status é informativo: no Marco 2 ela continuará recebendo
     * gente na lista de espera.
     */
    public void marcarComoLotada() {
        if (status == StatusPartida.ABERTA) {
            this.status = StatusPartida.LOTADA;
        }
    }

    /**
     * RN15: um cancelamento que libera vaga devolve a partida ao estado
     * ABERTA.
     */
    public void reabrirSeLotada() {
        if (status == StatusPartida.LOTADA) {
            this.status = StatusPartida.ABERTA;
        }
    }

    public UUID getId() {
        return id;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public LocalPartida getLocal() {
        return local;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public StatusPartida getStatus() {
        return status;
    }

    public OffsetDateTime getInscricoesAbremEm() {
        return inscricoesAbremEm;
    }

    public OffsetDateTime getInscricoesEncerramEm() {
        return inscricoesEncerramEm;
    }

    public Boolean getEscalaPublicada() {
        return escalaPublicada;
    }

    public Usuario getCriadoPor() {
        return criadoPor;
    }

    public Integer getVersao() {
        return versao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<Equipe> getEquipes() {
        return equipes;
    }
}
