package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.SituacaoAssociativa;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.CategoriaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.com.matheusassuncao.gestaojogos.repositorio.TokenRecuperacaoSenhaRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminJogadorControllerTest extends IntegracaoTest {

    private static final String SENHA = "senha12345";
    private static final String EMAIL_ADMIN = "admin.teste@clube.local";
    private static final String EMAIL_JOGADOR = "joao@teste.com";

    /** Série B, criada pelo seed da migration V3. */
    private static final long CATEGORIA_SERIE_B = 2L;

    @Autowired
    private TokenRecuperacaoSenhaRepository tokenRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PapelRepository papelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    @DisplayName("UC27: aprovação cria o perfil de jogador com categoria e situação")
    void aprovacaoCriaPerfilDeJogador() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("12345", CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL_JOGADOR))
                .andExpect(jsonPath("$.matriculaAssociado").value("12345"))
                .andExpect(jsonPath("$.situacaoAssociativa").value("REGULAR"))
                .andExpect(jsonPath("$.aprovadoEm").exists());

        Jogador jogador = jogadorRepository.findByUsuarioId(pendente.getId()).orElseThrow();

        assertThat(jogador.getSituacaoAssociativa()).isEqualTo(SituacaoAssociativa.REGULAR);
        assertThat(jogador.estaRegular()).isTrue();
        assertThat(jogador.getAprovadoPor()).isNotNull();
    }

    @Test
    @DisplayName("Após a aprovação o usuário fica ATIVO e recebe o papel JOGADOR")
    void aprovacaoAtivaUsuarioEAtribuiPapel() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao(null, CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isOk());

        MockHttpSession sessaoJogador = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/auth/eu").session(sessaoJogador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.papeis[0]").value("JOGADOR"));
    }

    @Test
    @DisplayName("Aprovar duas vezes o mesmo cadastro devolve 409")
    void aprovacaoDuplicada() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("12345", CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("99999", CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Usuário inexistente devolve 404")
    void aprovacaoDeUsuarioInexistente() throws Exception {
        criarAdministrador();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", UUID.randomUUID())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("12345", CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Categoria inexistente devolve 404")
    void aprovacaoComCategoriaInexistente() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("12345", 999L, "REGULAR")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A listagem traz apenas quem ainda não foi aprovado")
    void listagemDePendentes() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/jogadores/pendentes").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(EMAIL_JOGADOR));

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("12345", CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/jogadores/pendentes").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Jogador não acessa rotas de administrador: 403")
    void jogadorNaoAcessaRotaDeAdministrador() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("12345", CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isOk());

        MockHttpSession sessaoJogador = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/admin/jogadores/pendentes").session(sessaoJogador))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    @DisplayName("Usuário sem papel algum não acessa rotas de administrador: 403")
    void usuarioPendenteNaoAcessaRotaDeAdministrador() throws Exception {
        criarUsuarioPendente();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/admin/jogadores/pendentes").session(sessao))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rota de administrador exige autenticação: 401")
    void rotaDeAdministradorExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/admin/jogadores/pendentes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Categoria obrigatória: requisição sem ela devolve 400")
    void aprovacaoSemCategoria() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "situacaoAssociativa": "REGULAR" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.categoriaId").exists());
    }

    @Test
    @DisplayName("O seed da migration V3 cria as três séries")
    void seedDeCategorias() {
        assertThat(categoriaRepository.findByAtivoTrue())
                .hasSize(3)
                .extracting("nome")
                .containsExactlyInAnyOrder("Série A", "Série B", "Série C");
    }

    private Usuario criarAdministrador() {
        Usuario admin = new Usuario(
                "Administrador de Teste",
                EMAIL_ADMIN,
                passwordEncoder.encode(SENHA)
        );

        admin.ativar();
        admin.adicionarPapel(papelRepository.findByNome("ADMINISTRADOR").orElseThrow());

        return usuarioRepository.save(admin);
    }

    private Usuario criarUsuarioPendente() {
        return usuarioRepository.save(new Usuario(
                "Joao Silva",
                EMAIL_JOGADOR,
                passwordEncoder.encode(SENHA)
        ));
    }

    private String corpoDeAprovacao(String matricula, Long categoriaId, String situacao) {
        String matriculaJson = matricula == null ? "null" : "\"" + matricula + "\"";

        return """
                {
                  "matriculaAssociado": %s,
                  "categoriaId": %d,
                  "situacaoAssociativa": "%s"
                }
                """.formatted(matriculaJson, categoriaId, situacao);
    }

    private MockHttpSession autenticar(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);

        assertThat(sessao).as("o login deve criar uma sessão").isNotNull();

        return sessao;
    }
}
