package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.AprovarJogadorRequest;
import br.com.matheusassuncao.gestaojogos.dto.CadastroPendenteResponse;
import br.com.matheusassuncao.gestaojogos.dto.CategoriaResponse;
import br.com.matheusassuncao.gestaojogos.dto.JogadorResponse;
import br.com.matheusassuncao.gestaojogos.dto.RedefinirSenhaAdminRequest;
import br.com.matheusassuncao.gestaojogos.dto.UsuarioResumoResponse;
import br.com.matheusassuncao.gestaojogos.servico.AprovacaoJogadorService;
import br.com.matheusassuncao.gestaojogos.servico.JogadorAtivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final JogadorAtivoService jogadorAtivoService;

    public AdminJogadorController(AprovacaoJogadorService aprovacaoJogadorService,
                                  JogadorAtivoService jogadorAtivoService) {
        this.aprovacaoJogadorService = aprovacaoJogadorService;
        this.jogadorAtivoService = jogadorAtivoService;
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

    @GetMapping("/recusados")
    public List<CadastroPendenteResponse> listarRecusados() {
        return aprovacaoJogadorService.listarRecusados().stream()
                .map(CadastroPendenteResponse::de)
                .toList();
    }

    @PostMapping("/{usuarioId}/reabrir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reabrir(@PathVariable UUID usuarioId) {
        aprovacaoJogadorService.reabrir(usuarioId);
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

    @GetMapping("/ativos")
    public List<JogadorResponse> listarAtivos(@RequestParam(defaultValue = "") String busca) {
        return jogadorAtivoService.listarAtivos(busca);
    }

    @PostMapping("/{usuarioId}/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@PathVariable UUID usuarioId, @RequestBody @Valid RedefinirSenhaAdminRequest request) {
        jogadorAtivoService.redefinirSenha(
                usuarioId,
                request.novaSenha(),
                request.exigirTrocaNoProximoLogin()
        );
    }
}
