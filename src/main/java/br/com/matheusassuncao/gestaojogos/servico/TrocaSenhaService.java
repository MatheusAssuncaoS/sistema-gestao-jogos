package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.excecao.SenhaAtualInvalidaException;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Troca de senha pelo próprio usuário autenticado, exigindo a senha atual.
 * Separado de AuthService pelo mesmo motivo que RecuperacaoSenhaService já
 * é separado: mutação de senha fica em serviço próprio.
 */
@Service
public class TrocaSenhaService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public TrocaSenhaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void trocar(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new SenhaAtualInvalidaException("A senha atual informada está incorreta.");
        }

        // Sem essa checagem, quem tem senha provisória poderia "trocar" para a
        // mesma senha e limpar a flag — exatamente o que a troca obrigatória
        // existe para evitar.
        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.alterarSenha(passwordEncoder.encode(novaSenha));
        usuario.marcarSenhaTrocada();
    }
}
