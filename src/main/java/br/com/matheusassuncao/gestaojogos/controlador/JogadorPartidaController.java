package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dto.InscricaoResponse;
import br.com.matheusassuncao.gestaojogos.dto.PartidaResponse;
import br.com.matheusassuncao.gestaojogos.servico.InscricaoService;
import br.com.matheusassuncao.gestaojogos.servico.PartidaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * UC05 a UC08: o que o jogador faz com as partidas.
 */
@RestController
@RequestMapping("/api/partidas")
@PreAuthorize("hasRole('JOGADOR')")
public class JogadorPartidaController {

    private final PartidaService partidaService;
    private final InscricaoService inscricaoService;

    public JogadorPartidaController(PartidaService partidaService,
                                    InscricaoService inscricaoService) {
        this.partidaService = partidaService;
        this.inscricaoService = inscricaoService;
    }

    /**
     * UC05: consultar partidas disponíveis.
     */
    @GetMapping
    public List<PartidaResponse> disponiveis() {
        return partidaService.listarAbertas();
    }

    /**
     * UC06: inscrever-se em partida.
     */
    @PostMapping("/{partidaId}/inscricao")
    public ResponseEntity<InscricaoResponse> inscrever(
            @PathVariable UUID partidaId,
            @AuthenticationPrincipal UserDetails jogador) {

        InscricaoResponse resposta = InscricaoResponse.de(
                inscricaoService.inscrever(partidaId, jogador.getUsername())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /**
     * UC07: cancelar inscrição.
     */
    @DeleteMapping("/{partidaId}/inscricao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID partidaId,
                         @AuthenticationPrincipal UserDetails jogador) {

        inscricaoService.cancelar(partidaId, jogador.getUsername());
    }

    /**
     * UC08 e UC10: minhas inscrições.
     */
    @GetMapping("/minhas-inscricoes")
    public List<InscricaoResponse> minhasInscricoes(@AuthenticationPrincipal UserDetails jogador) {
        return inscricaoService.listarMinhasInscricoes(jogador.getUsername()).stream()
                .map(InscricaoResponse::de)
                .toList();
    }
}
