package br.com.matheusassuncao.gestaojogos.dominio;

import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inscricao")
public class Inscricao {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusInscricao status;

    /**
     * Define a ordem da lista de espera (RN12). A posição na fila é calculada
     * a partir deste campo, em vez de armazenada: assim um cancelamento não
     * exige renumerar todo mundo.
     */
    @Column(name = "data_solicitacao", nullable = false)
    private OffsetDateTime dataSolicitacao = OffsetDateTime.now();

    @Column(name = "data_confirmacao")
    private OffsetDateTime dataConfirmacao;

    @Column(name = "data_promocao")
    private OffsetDateTime dataPromocao;

    @Column(name = "data_cancelamento")
    private OffsetDateTime dataCancelamento;

    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por")
    private Usuario canceladoPor;

    protected Inscricao() {
        // exigido pelo JPA
    }

    public Inscricao(Partida partida, Jogador jogador, StatusInscricao status) {
        this.partida = partida;
        this.jogador = jogador;
        this.status = status;

        if (status == StatusInscricao.CONFIRMADA) {
            this.dataConfirmacao = OffsetDateTime.now();
        }
    }

    public void cancelar(Usuario responsavel, String motivo) {
        if (status == StatusInscricao.CANCELADA) {
            throw new RegraNegocioException("Esta inscrição já foi cancelada.");
        }

        this.status = StatusInscricao.CANCELADA;
        this.dataCancelamento = OffsetDateTime.now();
        this.canceladoPor = responsavel;
        this.motivoCancelamento = motivo;
    }

    /**
     * Usado na promoção da lista de espera (RN12, Marco 2).
     */
    public void promover() {
        if (status != StatusInscricao.LISTA_ESPERA) {
            throw new RegraNegocioException(
                    "Só é possível promover inscrições que estão na lista de espera."
            );
        }

        this.status = StatusInscricao.CONFIRMADA;
        this.dataConfirmacao = OffsetDateTime.now();
        this.dataPromocao = OffsetDateTime.now();
    }

    public boolean estaAtiva() {
        return status.estaAtiva();
    }

    public UUID getId() {
        return id;
    }

    public Partida getPartida() {
        return partida;
    }

    public Jogador getJogador() {
        return jogador;
    }

    public StatusInscricao getStatus() {
        return status;
    }

    public OffsetDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public OffsetDateTime getDataConfirmacao() {
        return dataConfirmacao;
    }

    public OffsetDateTime getDataPromocao() {
        return dataPromocao;
    }

    public OffsetDateTime getDataCancelamento() {
        return dataCancelamento;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public Usuario getCanceladoPor() {
        return canceladoPor;
    }
}
