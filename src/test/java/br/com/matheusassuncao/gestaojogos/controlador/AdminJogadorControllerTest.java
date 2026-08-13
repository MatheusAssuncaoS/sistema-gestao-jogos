package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Categoria;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @DisplayName("Recusar marca o usuário como RECUSADO e some da listagem de pendentes")
    void recusarMarcaUsuarioComoRecusado() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/recusar", pendente.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"));

        mockMvc.perform(get("/api/admin/jogadores/pendentes").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(jogadorRepository.findByUsuarioId(pendente.getId())).isEmpty();
    }

    @Test
    @DisplayName("Recusar duas vezes o mesmo cadastro devolve 409")
    void recusarCadastroJaDecidido() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/recusar", pendente.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/jogadores/{id}/recusar", pendente.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Recusar usuário inexistente devolve 404")
    void recusarUsuarioInexistente() throws Exception {
        criarAdministrador();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/recusar", UUID.randomUUID())
                        .session(sessaoAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Reabrir um cadastro recusado devolve 204 e o status vira PENDENTE")
    void reabrirCadastroRecusado() throws Exception {
        criarAdministrador();
        Usuario recusado = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/recusar", recusado.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/jogadores/{id}/reabrir", recusado.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isNoContent());

        Usuario atualizado = usuarioRepository.findById(recusado.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusUsuario.PENDENTE);
    }

    @Test
    @DisplayName("Reabrir um cadastro pendente devolve 409")
    void reabrirCadastroPendente() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/reabrir", pendente.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Reabrir um cadastro ativo devolve 409")
    void reabrirCadastroAtivo() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/aprovar", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeAprovacao("12345", CATEGORIA_SERIE_B, "REGULAR")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/jogadores/{id}/reabrir", pendente.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Reabrir um id inexistente devolve 404")
    void reabrirUsuarioInexistente() throws Exception {
        criarAdministrador();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/reabrir", UUID.randomUUID())
                        .session(sessaoAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Reabrir sem papel de administrador devolve 403")
    void reabrirSemPapelDeAdministrador() throws Exception {
        criarAdministrador();
        Usuario recusado = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/recusar", recusado.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk());

        MockHttpSession sessaoJogador = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/admin/jogadores/{id}/reabrir", recusado.getId())
                        .session(sessaoJogador))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ciclo completo: recusar, listar, reabrir e voltar a listar")
    void cicloDeRecusaEReabertura() throws Exception {
        criarAdministrador();
        Usuario usuario = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/recusar", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/jogadores/pendentes").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/admin/jogadores/recusados").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(EMAIL_JOGADOR));

        mockMvc.perform(post("/api/admin/jogadores/{id}/reabrir", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/jogadores/recusados").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/admin/jogadores/pendentes").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(EMAIL_JOGADOR));
    }

    @Test
    @DisplayName("Categorias ativas ficam disponíveis para o formulário de aprovação")
    void listagemDeCategorias() throws Exception {
        criarAdministrador();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/jogadores/categorias").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].nome",
                        containsInAnyOrder("Série A", "Série B", "Série C")));
    }

    @Test
    @DisplayName("O seed da migration V3 cria as três séries")
    void seedDeCategorias() {
        assertThat(categoriaRepository.findByAtivoTrue())
                .hasSize(3)
                .extracting("nome")
                .containsExactlyInAnyOrder("Série A", "Série B", "Série C");
    }

    @Test
    @DisplayName("Redefinir a senha de um jogador ativo devolve 204 e o login passa a usar a nova senha")
    void redefinirSenhaDeJogadorAtivo() throws Exception {
        criarAdministrador();
        Jogador jogador = criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", jogador.getUsuario().getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("novaSenha123", true)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "senha": "novaSenha123" }
                                """.formatted(EMAIL_JOGADOR)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "senha": "%s" }
                                """.formatted(EMAIL_JOGADOR, SENHA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("exigirTrocaNoProximoLogin verdadeiro marca a flag; falso não marca")
    void redefinirSenhaMarcaOuNaoAFlagDeTroca() throws Exception {
        criarAdministrador();
        Jogador comTroca = criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);
        Jogador semTroca = criarJogadorAtivo("Maria Souza", "maria@teste.com");

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", comTroca.getUsuario().getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("novaSenha123", true)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", semTroca.getUsuario().getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("outraSenha123", false)))
                .andExpect(status().isNoContent());

        assertThat(usuarioRepository.findById(comTroca.getUsuario().getId()).orElseThrow().isSenhaProvisoria())
                .isTrue();
        assertThat(usuarioRepository.findById(semTroca.getUsuario().getId()).orElseThrow().isSenhaProvisoria())
                .isFalse();
    }

    @Test
    @DisplayName("Redefinir a senha de um cadastro pendente devolve 409")
    void redefinirSenhaDeCadastroPendenteDevolve409() throws Exception {
        criarAdministrador();
        Usuario pendente = criarUsuarioPendente();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", pendente.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("novaSenha123", true)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Redefinir a senha de um cadastro recusado devolve 409")
    void redefinirSenhaDeCadastroRecusadoDevolve409() throws Exception {
        criarAdministrador();
        Usuario recusado = criarUsuarioPendente();
        recusado.recusar();
        usuarioRepository.save(recusado);

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", recusado.getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("novaSenha123", true)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Redefinir com senha de 7 caracteres devolve 400")
    void redefinirComSenhaCurtaDevolve400() throws Exception {
        criarAdministrador();
        Jogador jogador = criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", jogador.getUsuario().getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("1234567", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.novaSenha").exists());
    }

    @Test
    @DisplayName("Redefinir senha de id inexistente devolve 404")
    void redefinirSenhaDeIdInexistenteDevolve404() throws Exception {
        criarAdministrador();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", UUID.randomUUID())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("novaSenha123", true)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Redefinir senha sem papel de administrador devolve 403")
    void redefinirSenhaSemPapelDeAdministradorDevolve403() throws Exception {
        criarAdministrador();
        Jogador jogador = criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);

        MockHttpSession sessaoJogador = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", jogador.getUsuario().getId())
                        .session(sessaoJogador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("novaSenha123", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Redefinir a senha encerra a sessão que o jogador já tinha aberta")
    void redefinirSenhaEncerraSessaoAtiva() throws Exception {
        criarAdministrador();
        Jogador jogador = criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);

        MockHttpSession sessaoJogador = autenticar(EMAIL_JOGADOR);
        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/jogadores/{id}/redefinir-senha", jogador.getUsuario().getId())
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRedefinicao("novaSenha123", true)))
                .andExpect(status().isNoContent());

        // Confere o corpo, não só o status: sem um expiredSessionStrategy
        // customizado, o Spring Security responde via sendError(), que
        // depende da resolução de erro padrão (pode virar HTML) — aqui
        // precisa ser o mesmo ProblemDetail em JSON do resto da API. O texto
        // exato (com acento) também importa: escrever a resposta via
        // getWriter() em vez de getOutputStream() corrompe acentuação por
        // usar ISO-8859-1 em vez de UTF-8, e um `.exists()` não pegaria isso.
        mockMvc.perform(get("/api/auth/eu").session(sessaoJogador))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.detail").value("Sua sessão expirou. Entre novamente."));
    }

    @Test
    @DisplayName("Busca por trecho do nome traz o jogador correspondente")
    void buscaAtivosPorNome() throws Exception {
        criarAdministrador();
        criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);
        criarJogadorAtivo("Maria Souza", "maria@teste.com");

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/jogadores/ativos").param("busca", "joao").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(EMAIL_JOGADOR));
    }

    @Test
    @DisplayName("Busca por trecho do e-mail traz o jogador correspondente")
    void buscaAtivosPorEmail() throws Exception {
        criarAdministrador();
        criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);
        criarJogadorAtivo("Maria Souza", "maria@teste.com");

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/jogadores/ativos").param("busca", "maria@").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Maria Souza"));
    }

    @Test
    @DisplayName("Busca sem correspondência devolve lista vazia")
    void buscaAtivosSemCorrespondencia() throws Exception {
        criarAdministrador();
        criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/jogadores/ativos").param("busca", "ninguem").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Busca vazia devolve todos os ativos, sem pendentes nem recusados")
    void listagemDeAtivosNaoIncluiPendentesNemRecusados() throws Exception {
        criarAdministrador();
        criarJogadorAtivo("Joao Silva", EMAIL_JOGADOR);

        usuarioRepository.save(new Usuario("Pendente Teste", "pendente@teste.com", passwordEncoder.encode(SENHA)));

        Usuario recusado = usuarioRepository.save(
                new Usuario("Recusado Teste", "recusado@teste.com", passwordEncoder.encode(SENHA)));
        recusado.recusar();
        usuarioRepository.save(recusado);

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/jogadores/ativos").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(EMAIL_JOGADOR));
    }

    private Jogador criarJogadorAtivo(String nome, String email) {
        Usuario usuario = usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(SENHA)));
        usuario.ativar();
        usuario.adicionarPapel(papelRepository.findByNome("JOGADOR").orElseThrow());
        usuarioRepository.save(usuario);

        Categoria categoria = categoriaRepository.findById(CATEGORIA_SERIE_B).orElseThrow();
        Jogador jogador = new Jogador(usuario, null, categoria, SituacaoAssociativa.REGULAR, usuario);

        return jogadorRepository.save(jogador);
    }

    private String corpoDeRedefinicao(String novaSenha, boolean exigirTroca) {
        return """
                {
                  "novaSenha": "%s",
                  "exigirTrocaNoProximoLogin": %b
                }
                """.formatted(novaSenha, exigirTroca);
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
