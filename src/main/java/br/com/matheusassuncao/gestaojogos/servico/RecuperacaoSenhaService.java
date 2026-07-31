package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.TokenRecuperacaoSenha;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.TokenRecuperacaoSenhaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * UC03: recuperar senha.
 *
 * MVP: o token é registrado no log da aplicação em vez de enviado por e-mail.
 * O envio por SMTP é infraestrutura e ficou fora do escopo do MVP.
 */
@Service
public class RecuperacaoSenhaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);

    private static final int MINUTOS_DE_VALIDADE = 30;
    private static final int BYTES_DO_TOKEN = 32;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
                                   TokenRecuperacaoSenhaRepository tokenRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Gera um token de uso único. Não revela se o e-mail existe: a resposta
     * é sempre a mesma, para evitar enumeração de contas.
     */
    @Transactional
    public void solicitar(String email) {
        Optional<Usuario> possivelUsuario = usuarioRepository.findByEmailIgnoreCase(email);

        if (possivelUsuario.isEmpty()) {
            log.info("Recuperação de senha solicitada para e-mail não cadastrado.");
            return;
        }

        Usuario usuario = possivelUsuario.get();

        String token = gerarToken();
        OffsetDateTime expiraEm = OffsetDateTime.now().plusMinutes(MINUTOS_DE_VALIDADE);

        tokenRepository.save(new TokenRecuperacaoSenha(usuario, token, expiraEm));

        // TODO: substituir por envio de e-mail quando o SMTP for configurado
        log.warn("""
                        [MVP] Token de recuperação gerado.
                        E-mail: {}
                        Token: {}
                        Expira em: {}""",
                usuario.getEmail(), token, expiraEm);
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenRecuperacaoSenha tokenRecuperacao = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RegraNegocioException("Token inválido ou expirado."));

        if (!tokenRecuperacao.estaValido()) {
            throw new RegraNegocioException("Token inválido ou expirado.");
        }

        Usuario usuario = tokenRecuperacao.getUsuario();
        usuario.alterarSenha(passwordEncoder.encode(novaSenha));

        tokenRecuperacao.marcarComoUsado();

        log.info("Senha redefinida para o usuário {}.", usuario.getId());
    }

    private String gerarToken() {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
