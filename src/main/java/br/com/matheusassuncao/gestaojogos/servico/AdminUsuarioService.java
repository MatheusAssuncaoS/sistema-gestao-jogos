package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.AdminAtualizarUsuarioRequest;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.InscricaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminUsuarioService {
    private static final Logger log = LoggerFactory.getLogger(AdminUsuarioService.class);
    private final UsuarioRepository usuarioRepository;
    private final JogadorRepository jogadorRepository;
    private final InscricaoRepository inscricaoRepository;

    public AdminUsuarioService(UsuarioRepository usuarioRepository, JogadorRepository jogadorRepository,
                               InscricaoRepository inscricaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.jogadorRepository = jogadorRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    @Transactional
    public void excluir(UUID id, Long versao, String administrador) {
        Usuario usuario = buscar(id);
        validarVersao(usuario, versao);
        if (usuario.getEmail().equalsIgnoreCase(administrador)) {
            throw new RegraNegocioException("Você não pode excluir a própria conta.");
        }
        jogadorRepository.findByUsuarioId(id).ifPresent(jogador -> {
            if (inscricaoRepository.existsByJogadorId(jogador.getId())) {
                throw new RegraNegocioException("Este usuário possui histórico de inscrições e não pode ser excluído.");
            }
            jogadorRepository.delete(jogador);
        });
        usuarioRepository.delete(usuario);
        log.info("AUDITORIA admin={} acao=EXCLUIR_USUARIO usuario={} email={}.", administrador, id, usuario.getEmail());
    }

    @Transactional
    public Usuario atualizar(UUID id, AdminAtualizarUsuarioRequest request, String administrador) {
        Usuario usuario = buscar(id);
        validarVersao(usuario, request.versao());
        String email = request.email().trim().toLowerCase();
        if (!usuario.getEmail().equalsIgnoreCase(email) && usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
        usuario.alterarDados(request.nome().trim(), email);
        log.info("AUDITORIA admin={} acao=ATUALIZAR_USUARIO usuario={}.", administrador, id);
        return usuario;
    }

    @Transactional
    public Usuario alterarStatus(UUID id, StatusUsuario status, Long versao, String administrador) {
        Usuario usuario = buscar(id);
        validarVersao(usuario, versao);
        if (usuario.getEmail().equalsIgnoreCase(administrador) && status != StatusUsuario.ATIVO) {
            throw new RegraNegocioException("Você não pode bloquear ou inativar a própria conta.");
        }
        usuario.alterarStatus(status);
        log.info("AUDITORIA admin={} acao=ALTERAR_STATUS_USUARIO usuario={} status={}.", administrador, id, status);
        return usuario;
    }

    private Usuario buscar(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private void validarVersao(Usuario usuario, Long versao) {
        if (!usuario.getVersao().equals(versao)) {
            throw new RegraNegocioException("Este usuário foi atualizado por outro administrador. Recarregue os dados.");
        }
    }
}
