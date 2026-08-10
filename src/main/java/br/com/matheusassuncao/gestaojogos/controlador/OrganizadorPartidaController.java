package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dto.CriarPartidaRequest;
import br.com.matheusassuncao.gestaojogos.dto.EditarPartidaRequest;
import br.com.matheusassuncao.gestaojogos.dto.PartidaResponse;
import br.com.matheusassuncao.gestaojogos.servico.PartidaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * UC11: gerenciar partidas. Exige o papel ORGANIZADOR (UC19).
 */
@RestController
@RequestMapping("/api/organizador/partidas")
@PreAuthorize("hasRole('ORGANIZADOR')")
public class OrganizadorPartidaController {

    private final PartidaService partidaService;

    public OrganizadorPartidaController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    @GetMapping
    public List<PartidaResponse> listar() {
        return partidaService.listarFuturas();
    }

    @GetMapping("/{partidaId}")
    public PartidaResponse detalhar(@PathVariable UUID partidaId) {
        return partidaService.detalhar(partidaId);
    }

    @PostMapping
    public ResponseEntity<PartidaResponse> criar(@RequestBody @Valid CriarPartidaRequest request,
                                                 @AuthenticationPrincipal UserDetails organizador) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(partidaService.criar(request, organizador.getUsername()));
    }

    @PutMapping("/{partidaId}")
    public PartidaResponse editar(@PathVariable UUID partidaId,
                                  @RequestBody @Valid EditarPartidaRequest request) {
        return partidaService.editar(partidaId, request);
    }

    @PostMapping("/{partidaId}/abrir")
    public PartidaResponse abrir(@PathVariable UUID partidaId) {
        return partidaService.abrir(partidaId);
    }

    @PostMapping("/{partidaId}/cancelar")
    public PartidaResponse cancelar(@PathVariable UUID partidaId) {
        return partidaService.cancelar(partidaId);
    }
}
