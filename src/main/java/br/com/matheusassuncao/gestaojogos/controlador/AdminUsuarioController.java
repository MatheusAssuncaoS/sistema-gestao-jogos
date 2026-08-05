package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.UsuarioResumoResponse;
import br.com.matheusassuncao.gestaojogos.servico.GestaoPapelService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public AdminUsuarioController(GestaoPapelService gestaoPapelService) {
        this.gestaoPapelService = gestaoPapelService;
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
