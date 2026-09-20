package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
@EnableScheduling
public class AberturaInscricoesAgendada {
    private final PartidaRepository partidas;

    public AberturaInscricoesAgendada(PartidaRepository partidas) {
        this.partidas = partidas;
    }

    // Também recupera aberturas pendentes após uma reinicialização do servidor.
    @Scheduled(fixedDelayString = "${partidas.inscricoes.verificacao-ms:15000}")
    public void abrirInscricoes() {
        partidas.abrirInscricoesAgendadas(OffsetDateTime.now());
    }
}
