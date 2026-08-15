package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.TokenRecuperacaoSenha;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.TokenRecuperacaoSenhaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecuperacaoSenhaTest extends IntegracaoTest {

    private static final String SENHA_ORIGINAL = "senha12345";
    private static final String SENHA_NOVA = "NovaSenha123!";
    private static final String EMAIL = "matheus@teste.com";

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacaoSenhaRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    @DisplayName("UC03: solicitação gera token válido para o usuário")
    void solicitacaoGeraToken() throws Exception {
        criarUsuario();

        solicitarRecuperacao(EMAIL).andExpect(status().isNoContent());

        assertThat(tokenRepository.findAll())
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.estaValido()).isTrue();
                    assertThat(token.getUsadoEm()).isNull();
                    assertThat(token.getExpiraEm()).isAfter(OffsetDateTime.now());
                });
    }

    @Test
    @DisplayName("E-mail inexistente devolve 204 e não gera token")
    void solicitacaoParaEmailInexistente() throws Exception {
        solicitarRecuperacao("ninguem@teste.com").andExpect(status().isNoContent());

        assertThat(tokenRepository.findAll())
                .as("não deve vazar a existência da conta nem criar token")
                .isEmpty();
    }

    @Test
    @DisplayName("Token válido redefine a senha e permite login com a nova")
    void redefinicaoComTokenValido() throws Exception {
        criarUsuario();
        solicitarRecuperacao(EMAIL);

        String token = tokenRepository.findAll().getFirst().getToken();

        redefinir(token, SENHA_NOVA).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(EMAIL, SENHA_NOVA)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A senha antiga deixa de funcionar após a redefinição")
    void senhaAntigaDeixaDeFuncionar() throws Exception {
        criarUsuario();
        solicitarRecuperacao(EMAIL);

        String token = tokenRepository.findAll().getFirst().getToken();
        redefinir(token, SENHA_NOVA).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(EMAIL, SENHA_ORIGINAL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("O token é de uso único: a segunda tentativa devolve 409")
    void tokenNaoPodeSerReutilizado() throws Exception {
        criarUsuario();
        solicitarRecuperacao(EMAIL);

        String token = tokenRepository.findAll().getFirst().getToken();

        redefinir(token, SENHA_NOVA).andExpect(status().isNoContent());
        redefinir(token, "OutraSenha123!").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Token expirado devolve 409")
    void tokenExpirado() throws Exception {
        Usuario usuario = criarUsuario();

        tokenRepository.save(new TokenRecuperacaoSenha(
                usuario,
                "token-expirado",
                OffsetDateTime.now().minusMinutes(1)
        ));

        redefinir("token-expirado", SENHA_NOVA).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Token inexistente devolve 409")
    void tokenInexistente() throws Exception {
        criarUsuario();

        redefinir("token-que-nao-existe", SENHA_NOVA).andExpect(status().isConflict());
    }

    private Usuario criarUsuario() {
        return usuarioRepository.save(new Usuario(
                "Matheus",
                EMAIL,
                passwordEncoder.encode(SENHA_ORIGINAL)
        ));
    }

    private org.springframework.test.web.servlet.ResultActions solicitarRecuperacao(String email)
            throws Exception {
        return mockMvc.perform(post("/api/auth/recuperacao-senha/solicitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s" }
                        """.formatted(email)));
    }

    private org.springframework.test.web.servlet.ResultActions redefinir(String token, String senha)
            throws Exception {
        return mockMvc.perform(post("/api/auth/recuperacao-senha/redefinir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "token": "%s",
                          "novaSenha": "%s"
                        }
                        """.formatted(token, senha)));
    }
}
