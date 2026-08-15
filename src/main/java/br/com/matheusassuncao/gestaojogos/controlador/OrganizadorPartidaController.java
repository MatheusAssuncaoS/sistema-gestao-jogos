package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dto.CategoriaResponse;
import br.com.matheusassuncao.gestaojogos.dto.CriarPartidaRequest;
import br.com.matheusassuncao.gestaojogos.dto.EditarPartidaRequest;
import br.com.matheusassuncao.gestaojogos.dto.InscritoResponse;
import br.com.matheusassuncao.gestaojogos.dto.LocalPartidaResponse;
import br.com.matheusassuncao.gestaojogos.dto.ModalidadeResponse;
import br.com.matheusassuncao.gestaojogos.dto.PartidaResponse;
import br.com.matheusassuncao.gestaojogos.servico.InscricaoService;
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
    private final InscricaoService inscricaoService;

    public OrganizadorPartidaController(PartidaService partidaService,
                                        InscricaoService inscricaoService) {
        this.partidaService = partidaService;
        this.inscricaoService = inscricaoService;
    }

    /**
     * Listar, detalhar e consultar inscritos também são permitidos ao
     * administrador. Ele também pode criar rascunhos, mas editar, abrir e
     * cancelar continuam exclusivos do organizador. @PreAuthorize no método
     * sobrescreve o da classe somente nos endpoints indicados.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMINISTRADOR')")
    public List<PartidaResponse> listar() {
        return partidaService.listarFuturas();
    }

    @GetMapping("/modalidades")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMINISTRADOR')")
    public List<ModalidadeResponse> modalidades() {
        return partidaService.listarModalidadesAtivas();
    }

    @GetMapping("/locais")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMINISTRADOR')")
    public List<LocalPartidaResponse> locais() {
        return partidaService.listarLocaisAtivos();
    }

    @GetMapping("/categorias")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMINISTRADOR')")
    public List<CategoriaResponse> categorias() {
        return partidaService.listarCategoriasAtivas();
    }

    @GetMapping("/{partidaId}")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMINISTRADOR')")
    public PartidaResponse detalhar(@PathVariable UUID partidaId) {
        return partidaService.detalhar(partidaId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMINISTRADOR')")
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
    public PartidaResponse cancelar(@PathVariable UUID partidaId,
                                    @AuthenticationPrincipal UserDetails organizador) {
        return partidaService.cancelar(partidaId, organizador.getUsername());
    }

    /**
     * UC17: consultar inscrições.
     */
    @GetMapping("/{partidaId}/inscritos")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMINISTRADOR')")
    public List<InscritoResponse> inscritos(@PathVariable UUID partidaId) {
        return inscricaoService.listarInscritos(partidaId).stream()
                .map(InscritoResponse::de)
                .toList();
    }
}
