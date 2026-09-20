package br.com.matheusassuncao.gestaojogos.dominio;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class AberturaInscricoesTest {
    private final OffsetDateTime agora = OffsetDateTime.parse("2026-09-13T14:00:00-03:00");

    private Partida partida(OffsetDateTime abre, OffsetDateTime encerra, OffsetDateTime inicio) {
        return new Partida(null, null, null, inicio, 16, abre, encerra, null);
    }

    @Test void abreNoInicioExatoDoPrazo() {
        var p = partida(agora, agora.plusHours(2), agora.plusHours(3));
        p.abrirInscricoesSeNoPrazo(agora);
        assertThat(p.getStatus()).isEqualTo(StatusPartida.ABERTA);
    }
    @Test void abreDentroDoPrazoMesmoSemEncerramento() {
        var p = partida(agora.minusHours(1), null, agora.plusHours(3));
        p.abrirInscricoesSeNoPrazo(agora);
        p.abrirInscricoesSeNoPrazo(agora);
        assertThat(p.getStatus()).isEqualTo(StatusPartida.ABERTA);
    }
    @Test void naoAbreAntesDoPrazoOuSemAberturaConfigurada() {
        for (var p : java.util.List.of(partida(agora.plusHours(1), null, agora.plusHours(3)), partida(null, null, agora.plusHours(3)))) {
            p.abrirInscricoesSeNoPrazo(agora);
            assertThat(p.getStatus()).isEqualTo(StatusPartida.RASCUNHO);
        }
    }
    @Test void naoAbreNoEncerramentoOuDepoisDoInicioDaPartida() {
        for (var p : java.util.List.of(partida(agora.minusHours(1), agora, agora.plusHours(3)), partida(agora.minusHours(1), null, agora))) {
            p.abrirInscricoesSeNoPrazo(agora);
            assertThat(p.getStatus()).isEqualTo(StatusPartida.RASCUNHO);
        }
    }
    @Test void naoReabreCancelada() {
        var p = partida(agora, null, agora.plusHours(3));
        p.cancelar();
        p.abrirInscricoesSeNoPrazo(agora);
        assertThat(p.getStatus()).isEqualTo(StatusPartida.CANCELADA);
    }
}
