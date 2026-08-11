package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dto.DiaFuncionamentoRequest;
import br.com.matheusassuncao.gestaojogos.dto.DiaFuncionamentoResponse;
import br.com.matheusassuncao.gestaojogos.dto.ExcecaoCalendarioRequest;
import br.com.matheusassuncao.gestaojogos.dto.ExcecaoCalendarioResponse;
import br.com.matheusassuncao.gestaojogos.servico.CalendarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * UC21: gerenciar calendário.
 */
@RestController
@RequestMapping("/api/admin/calendario")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminCalendarioController {

    private final CalendarioService calendarioService;

    public AdminCalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @GetMapping("/dias-funcionamento")
    public List<DiaFuncionamentoResponse> listarDias() {
        return calendarioService.listarDiasDeFuncionamento().stream()
                .map(DiaFuncionamentoResponse::de)
                .toList();
    }

    @PostMapping("/dias-funcionamento")
    public ResponseEntity<DiaFuncionamentoResponse> adicionarDia(
            @RequestBody @Valid DiaFuncionamentoRequest request) {

        DiaFuncionamentoResponse resposta = DiaFuncionamentoResponse.de(
                calendarioService.adicionarDiaDeFuncionamento(
                        request.diaDaSemana(),
                        request.horario()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @DeleteMapping("/dias-funcionamento/{diaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerDia(@PathVariable UUID diaId) {
        calendarioService.removerDiaDeFuncionamento(diaId);
    }

    @GetMapping("/excecoes")
    public List<ExcecaoCalendarioResponse> listarExcecoes() {
        return calendarioService.listarExcecoesVigentes().stream()
                .map(ExcecaoCalendarioResponse::de)
                .toList();
    }

    @PostMapping("/excecoes")
    public ResponseEntity<ExcecaoCalendarioResponse> adicionarExcecao(
            @RequestBody @Valid ExcecaoCalendarioRequest request) {

        ExcecaoCalendarioResponse resposta = ExcecaoCalendarioResponse.de(
                calendarioService.adicionarExcecao(
                        request.descricao(),
                        request.tipo(),
                        request.inicio(),
                        request.fim()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @DeleteMapping("/excecoes/{excecaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerExcecao(@PathVariable UUID excecaoId) {
        calendarioService.removerExcecao(excecaoId);
    }
}
