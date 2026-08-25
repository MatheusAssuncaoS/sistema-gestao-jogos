package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dto.AtualizarEstadoArbitragemRequest;
import br.com.matheusassuncao.gestaojogos.dto.EstadoArbitragemResponse;
import br.com.matheusassuncao.gestaojogos.dto.InscritoResponse;
import br.com.matheusassuncao.gestaojogos.dto.PartidaResponse;
import br.com.matheusassuncao.gestaojogos.servico.ArbitragemService;
import br.com.matheusassuncao.gestaojogos.servico.InscricaoService;
import br.com.matheusassuncao.gestaojogos.servico.PartidaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/arbitro/partidas")
@PreAuthorize("hasRole('ARBITRO')")
public class ArbitroPartidaController {

    private final PartidaService partidaService;
    private final InscricaoService inscricaoService;
    private final ArbitragemService arbitragemService;

    public ArbitroPartidaController(PartidaService partidaService,
                                    InscricaoService inscricaoService,
                                    ArbitragemService arbitragemService) {
        this.partidaService = partidaService;
        this.inscricaoService = inscricaoService;
        this.arbitragemService = arbitragemService;
    }

    @GetMapping
    public List<PartidaResponse> partidas() {
        return partidaService.listarAbertas();
    }

    @GetMapping("/{partidaId}/participantes")
    public List<InscritoResponse> participantes(@PathVariable UUID partidaId) {
        return inscricaoService.listarInscritos(partidaId).stream()
                .map(InscritoResponse::de)
                .toList();
    }

    @GetMapping("/{partidaId}/estado")
    public EstadoArbitragemResponse estado(@PathVariable UUID partidaId) {
        return arbitragemService.obter(partidaId);
    }

    @PutMapping("/{partidaId}/estado")
    public EstadoArbitragemResponse atualizarEstado(@PathVariable UUID partidaId,
                                                     @Valid @RequestBody AtualizarEstadoArbitragemRequest request) {
        return arbitragemService.atualizar(partidaId, request);
    }
}
