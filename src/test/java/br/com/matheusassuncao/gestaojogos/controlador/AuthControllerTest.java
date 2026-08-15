package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.TokenRecuperacaoSenhaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.springframework.mock.web.MockHttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegracaoTest {

    private static final String SENHA_VALIDA = "Senha12345!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacaoSenhaRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    @DisplayName("UC01: cadastro cria usuário PENDENTE, sem papéis e sem expor a senha")
    void cadastroValido() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Matheus",
                                  "email": "matheus@teste.com",
                                  "senha": "%s"
                                }
                                """.formatted(SENHA_VALIDA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("matheus@teste.com"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.papeis").isEmpty())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmailIgnoreCase("matheus@teste.com").orElseThrow();

        assertThat(salvo.getStatus()).isEqualTo(StatusUsuario.PENDENTE);
        assertThat(salvo.getSenhaHash())
                .as("a senha deve ser persistida com hash, nunca em texto puro")
                .isNotEqualTo(SENHA_VALIDA);
    }

    @Test
    @DisplayName("RN01: e-mail já cadastrado devolve 409")
    void cadastroComEmailDuplicado() throws Exception {
        criarUsuario("matheus@teste.com");

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Outro",
                                  "email": "matheus@teste.com",
                                  "senha": "%s"
                                }
                                """.formatted(SENHA_VALIDA)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Campos inválidos devolvem 400 com a lista de erros")
    void cadastroComCamposInvalidos() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "email": "nao-e-email",
                                  "senha": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    @DisplayName("UC02: login válido devolve 200 e cria sessão")
    void loginValido() throws Exception {
        criarUsuario("matheus@teste.com");

        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "matheus@teste.com",
                                  "senha": "%s"
                                }
                                """.formatted(SENHA_VALIDA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("matheus@teste.com"))
                .andReturn();

        assertThat(resultado.getRequest().getSession(false))
                .as("o login deve criar uma sessão")
                .isNotNull();
    }

    @Test
    @DisplayName("Senha incorreta devolve 401 sem revelar se o e-mail existe")
    void loginComSenhaIncorreta() throws Exception {
        criarUsuario("matheus@teste.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "matheus@teste.com",
                                  "senha": "senha-errada"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("E-mail inexistente devolve a mesma resposta da senha incorreta")
    void loginComEmailInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ninguem@teste.com",
                                  "senha": "%s"
                                }
                                """.formatted(SENHA_VALIDA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("Rota protegida sem sessão devolve 401")
    void consultaSemSessao() throws Exception {
        mockMvc.perform(get("/api/auth/eu"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A sessão do login continua válida na requisição seguinte")
    void sessaoPersisteEntreRequisicoes() throws Exception {
        criarUsuario("matheus@teste.com");

        MockHttpSession sessao = autenticarEObterSessao("matheus@teste.com");

        mockMvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("matheus@teste.com"));
    }

    @Test
    @DisplayName("Logout invalida a sessão")
    void logoutInvalidaSessao() throws Exception {
        criarUsuario("matheus@teste.com");

        MockHttpSession sessao = autenticarEObterSessao("matheus@teste.com");

        mockMvc.perform(post("/api/auth/logout").session(sessao))
                .andExpect(status().isNoContent());

        assertThat(sessao.isInvalid())
                .as("o logout deve invalidar a sessão")
                .isTrue();
    }

    @Test
    @DisplayName("Login e /eu trazem senhaProvisoria no corpo")
    void loginTrazSenhaProvisoria() throws Exception {
        criarUsuario("matheus@teste.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "matheus@teste.com", "senha": "%s" }
                                """.formatted(SENHA_VALIDA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senhaProvisoria").value(false));
    }

    @Test
    @DisplayName("Trocar senha com a senha atual errada devolve 400, não 401")
    void trocarSenhaComSenhaAtualErrada() throws Exception {
        criarUsuario("matheus@teste.com");

        MockHttpSession sessao = autenticarEObterSessao("matheus@teste.com");

        // 400, não 401: o usuário está autenticado, só errou a senha atual.
        // Um 401 aqui arriscaria ser tratado como sessão expirada pelo front.
        mockMvc.perform(post("/api/auth/trocar-senha")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "senhaAtual": "senha-errada", "novaSenha": "NovaSenha123!" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senhaAtual").exists());
    }

    @Test
    @DisplayName("Trocar para a mesma senha atual devolve 409")
    void trocarSenhaPelaMesmaSenhaAtual() throws Exception {
        criarUsuario("matheus@teste.com");

        MockHttpSession sessao = autenticarEObterSessao("matheus@teste.com");

        mockMvc.perform(post("/api/auth/trocar-senha")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "senhaAtual": "%s", "novaSenha": "%s" }
                                """.formatted(SENHA_VALIDA, SENHA_VALIDA)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Trocar senha com dados corretos devolve 204 e limpa a flag de senha provisória")
    void trocarSenhaComDadosCorretos() throws Exception {
        Usuario usuario = new Usuario("Matheus", "matheus@teste.com", passwordEncoder.encode(SENHA_VALIDA));
        usuario.redefinirSenha(passwordEncoder.encode(SENHA_VALIDA), true);
        usuarioRepository.save(usuario);

        MockHttpSession sessao = autenticarEObterSessao("matheus@teste.com");

        mockMvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senhaProvisoria").value(true));

        mockMvc.perform(post("/api/auth/trocar-senha")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "senhaAtual": "%s", "novaSenha": "NovaSenha123!" }
                                """.formatted(SENHA_VALIDA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senhaProvisoria").value(false));
    }

    @Test
    @DisplayName("UC04: atualização de dados altera o nome do usuário autenticado")
    void atualizarDados() throws Exception {
        criarUsuario("matheus@teste.com");

        MockHttpSession sessao = autenticarEObterSessao("matheus@teste.com");

        mockMvc.perform(put("/api/usuarios/eu")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Matheus Assuncao",
                                  "email": "matheus@teste.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Matheus Assuncao"));
    }

    private void criarUsuario(String email) {
        usuarioRepository.save(new Usuario(
                "Matheus",
                email,
                passwordEncoder.encode(SENHA_VALIDA)
        ));
    }

    private MockHttpSession autenticarEObterSessao(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(email, SENHA_VALIDA)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);

        assertThat(sessao).as("o login deve criar uma sessão").isNotNull();

        return sessao;
    }
}
