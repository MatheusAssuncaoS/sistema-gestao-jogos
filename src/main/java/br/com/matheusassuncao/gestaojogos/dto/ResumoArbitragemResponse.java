package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.StatusArbitragem;

public record ResumoArbitragemResponse(
        StatusArbitragem status,
        int golsAmarelo,
        int golsAzul,
        int totalGols,
        int totalPunicoes,
        int cartoesAmarelos,
        int cartoesVermelhos,
        int expulsos,
        int acrescimos,
        int segundos
) {
}
