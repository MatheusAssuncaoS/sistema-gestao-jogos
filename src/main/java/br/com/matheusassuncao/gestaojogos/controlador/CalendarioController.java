package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.servico.CalendarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consulta do calendário por qualquer usuário autenticado: o jogador precisa
 * saber quando há partidas, e o organizador, quando pode agendar.
 */
@RestController
@RequestMapping("/api/calendario")
public class CalendarioController {

    private static final int PADRAO_DE_DIAS = 30;
    private static final int MAXIMO_DE_DIAS = 90;

    private final CalendarioService calendarioService;

    public CalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @GetMapping("/horarios-disponiveis")
    public List<OffsetDateTime> horariosDisponiveis(
            @RequestParam(defaultValue = "" + PADRAO_DE_DIAS) int dias) {

        return calendarioService.listarProximosHorarios(Math.min(dias, MAXIMO_DE_DIAS));
    }
}
