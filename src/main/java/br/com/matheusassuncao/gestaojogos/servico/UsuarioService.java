package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.AtualizarDadosRequest;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC04: atualizar dados pessoais.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario atualizarDados(String emailAtual, AtualizarDadosRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailAtual)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        String novoEmail = request.email().trim().toLowerCase();

        boolean trocouDeEmail = !usuario.getEmail().equalsIgnoreCase(novoEmail);

        if (trocouDeEmail && usuarioRepository.existsByEmailIgnoreCase(novoEmail)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }

        usuario.alterarDados(request.nome().trim(), novoEmail);

        return usuario;
    }
}
