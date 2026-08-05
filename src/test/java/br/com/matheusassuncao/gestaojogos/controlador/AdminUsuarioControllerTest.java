package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUsuarioControllerTest extends IntegracaoTest {

    private static final String SENHA = "senha12345";
    private static final String EMAIL_ADMIN = "admin.teste@clube.local";
    private static final String EMAIL_COMUM = "maria@teste.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PapelRepository papelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("UC19: administrador concede o papel de organizador")
    void concederOrganizador() throws Exception {
        criarAdministrador();
        Usuario usuario = criarUsuarioComum();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL_COMUM))
                .andExpect(jsonPath("$.papeis").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    @DisplayName("O papel concedido aparece na sessão do próprio usuário")
    void papelAparecerNoLoginSeguinte() throws Exception {
        criarAdministrador();
        Usuario usuario = criarUsuarioComum();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk());

        MockHttpSession sessaoUsuario = autenticar(EMAIL_COMUM);

        mockMvc.perform(get("/api/auth/eu").session(sessaoUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis[0]").value("ORGANIZADOR"));
    }

    @Test
    @DisplayName("Conceder duas vezes é idempotente")
    void concessaoIdempotente() throws Exception {
        criarAdministrador();
        Usuario usuario = criarUsuarioComum();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis.length()").value(1));
    }

    @Test
    @DisplayName("Revogação remove o papel")
    void revogarOrganizador() throws Exception {
        criarAdministrador();
        Usuario usuario = criarUsuarioComum();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis").isEmpty());

        Usuario recarregado = usuarioRepository.findById(usuario.getId()).orElseThrow();

        assertThat(recarregado.possuiPapel("ORGANIZADOR")).isFalse();
    }

    @Test
    @DisplayName("Revogar de quem não tem o papel não devolve erro")
    void revogacaoIdempotente() throws Exception {
        criarAdministrador();
        Usuario usuario = criarUsuarioComum();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(delete("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis").isEmpty());
    }

    @Test
    @DisplayName("Usuário inexistente devolve 404")
    void usuarioInexistente() throws Exception {
        criarAdministrador();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/usuarios/{id}/organizador", UUID.randomUUID())
                        .session(sessaoAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A listagem mostra os usuários com seus papéis")
    void listagemDeUsuarios() throws Exception {
        criarAdministrador();
        criarUsuarioComum();

        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/usuarios").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Usuário comum não gerencia papéis: 403")
    void usuarioComumNaoGerenciaPapeis() throws Exception {
        criarAdministrador();
        Usuario usuario = criarUsuarioComum();

        MockHttpSession sessaoUsuario = autenticar(EMAIL_COMUM);

        mockMvc.perform(post("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoUsuario))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/usuarios/{id}/organizador", usuario.getId())
                        .session(sessaoUsuario))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Gestão de papéis exige autenticação: 401")
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized());
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

    private Usuario criarUsuarioComum() {
        Usuario usuario = new Usuario(
                "Maria Souza",
                EMAIL_COMUM,
                passwordEncoder.encode(SENHA)
        );

        usuario.ativar();

        return usuarioRepository.save(usuario);
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
