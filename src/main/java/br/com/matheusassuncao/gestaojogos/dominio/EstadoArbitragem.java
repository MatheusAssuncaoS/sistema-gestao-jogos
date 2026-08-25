package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "estado_arbitragem")
public class EstadoArbitragem {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false, unique = true)
    private Partida partida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusArbitragem status = StatusArbitragem.PREPARACAO;

    @Column(name = "segundos_base", nullable = false)
    private Integer segundosBase = 0;

    @Column(name = "cronometro_iniciado_em")
    private OffsetDateTime cronometroIniciadoEm;

    @Column(name = "dados_json", nullable = false, columnDefinition = "TEXT")
    private String dadosJson;

    @Version
    @Column(nullable = false)
    private Integer versao;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    protected EstadoArbitragem() {
    }

    public EstadoArbitragem(Partida partida, String dadosJson) {
        this.partida = partida;
        this.dadosJson = dadosJson;
    }

    public void atualizar(StatusArbitragem novoStatus, int segundos, String novosDados) {
        this.status = novoStatus;
        this.segundosBase = segundos;
        this.cronometroIniciadoEm = novoStatus == StatusArbitragem.EM_ANDAMENTO
                ? OffsetDateTime.now()
                : null;
        this.dadosJson = novosDados;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public int segundosAtuais() {
        if (status != StatusArbitragem.EM_ANDAMENTO || cronometroIniciadoEm == null) {
            return segundosBase;
        }
        long decorridos = Duration.between(cronometroIniciadoEm, OffsetDateTime.now()).getSeconds();
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, segundosBase.longValue() + Math.max(0, decorridos)));
    }

    public Partida getPartida() { return partida; }
    public StatusArbitragem getStatus() { return status; }
    public String getDadosJson() { return dadosJson; }
    public Integer getVersao() { return versao; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
