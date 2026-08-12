package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.AprovarJogadorRequest;
import br.com.matheusassuncao.gestaojogos.dto.CadastroPendenteResponse;
import br.com.matheusassuncao.gestaojogos.dto.CategoriaResponse;
import br.com.matheusassuncao.gestaojogos.dto.JogadorResponse;
import br.com.matheusassuncao.gestaojogos.dto.UsuarioResumoResponse;
import br.com.matheusassuncao.gestaojogos.servico.AprovacaoJogadorService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * UC27: administração de cadastros de jogadores.
 *
 * Todo o controller exige o papel ADMINISTRADOR. É a primeira autorização
 * por perfil do sistema: até aqui bastava estar autenticado.
 */
@RestController
@RequestMapping("/api/admin/jogadores")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminJogadorController {

    private final AprovacaoJogadorService aprovacaoJogadorService;

    public AdminJogadorController(AprovacaoJogadorService aprovacaoJogadorService) {
        this.aprovacaoJogadorService = aprovacaoJogadorService;
    }

    @GetMapping("/pendentes")
    public List<CadastroPendenteResponse> listarPendentes() {
        return aprovacaoJogadorService.listarPendentes().stream()
                .map(CadastroPendenteResponse::de)
                .toList();
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return aprovacaoJogadorService.listarCategoriasAtivas().stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    @PostMapping("/{usuarioId}/recusar")
    public UsuarioResumoResponse recusar(@PathVariable UUID usuarioId) {
        Usuario usuario = aprovacaoJogadorService.recusar(usuarioId);

        return UsuarioResumoResponse.de(usuario);
    }

    @PostMapping("/{usuarioId}/aprovar")
    public JogadorResponse aprovar(@PathVariable UUID usuarioId,
                                   @RequestBody @Valid AprovarJogadorRequest request,
                                   @AuthenticationPrincipal UserDetails administrador) {

        Jogador jogador = aprovacaoJogadorService.aprovar(
                usuarioId,
                request,
                administrador.getUsername()
        );

        return JogadorResponse.de(jogador);
    }
}
