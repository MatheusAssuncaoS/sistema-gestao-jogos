package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.UsuarioResumoResponse;
import br.com.matheusassuncao.gestaojogos.dto.AdminAlterarStatusUsuarioRequest;
import br.com.matheusassuncao.gestaojogos.dto.AdminAtualizarUsuarioRequest;
import br.com.matheusassuncao.gestaojogos.servico.AdminUsuarioService;
import br.com.matheusassuncao.gestaojogos.servico.GestaoPapelService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * UC19: gerenciar organizadores.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

    private final GestaoPapelService gestaoPapelService;
    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(GestaoPapelService gestaoPapelService, AdminUsuarioService adminUsuarioService) {
        this.gestaoPapelService = gestaoPapelService;
        this.adminUsuarioService = adminUsuarioService;
    }

    @PutMapping("/{usuarioId}")
    public UsuarioResumoResponse atualizar(@PathVariable UUID usuarioId,
                                            @RequestBody @Valid AdminAtualizarUsuarioRequest request,
                                            Principal principal) {
        return UsuarioResumoResponse.de(adminUsuarioService.atualizar(usuarioId, request, principal.getName()));
    }

    @PatchMapping("/{usuarioId}/status")
    public UsuarioResumoResponse alterarStatus(@PathVariable UUID usuarioId,
                                                @RequestBody @Valid AdminAlterarStatusUsuarioRequest request,
                                                Principal principal) {
        return UsuarioResumoResponse.de(adminUsuarioService.alterarStatus(
                usuarioId, request.status(), request.versao(), principal.getName()));
    }

    @DeleteMapping("/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID usuarioId, @RequestParam Long versao, Principal principal) {
        adminUsuarioService.excluir(usuarioId, versao, principal.getName());
    }

    @GetMapping
    public List<UsuarioResumoResponse> listar() {
        return gestaoPapelService.listarUsuarios().stream()
                .map(UsuarioResumoResponse::de)
                .toList();
    }

    @PostMapping("/{usuarioId}/organizador")
    public UsuarioResumoResponse conceder(@PathVariable UUID usuarioId) {
        Usuario usuario = gestaoPapelService.concederOrganizador(usuarioId);

        return UsuarioResumoResponse.de(usuario);
    }

    @DeleteMapping("/{usuarioId}/organizador")
    public UsuarioResumoResponse revogar(@PathVariable UUID usuarioId) {
        Usuario usuario = gestaoPapelService.revogarOrganizador(usuarioId);

        return UsuarioResumoResponse.de(usuario);
    }
}
