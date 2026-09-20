package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.servico.AdminConfiguracaoService;
import br.com.matheusassuncao.gestaojogos.repositorio.LocalPartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ModalidadeRepository;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class LocaisModalidadesTest extends IntegracaoTest {
    @Autowired AdminConfiguracaoService servico;
    @Autowired LocalPartidaRepository locais;
    @Autowired ModalidadeRepository modalidades;
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PapelRepository papeis;
    @Autowired PasswordEncoder encoder;
    private MockHttpSession autenticar() throws Exception {
        var admin = new Usuario("Administrador", "vinculos@teste.local", encoder.encode("senha12345"));
        admin.ativar();
        admin.adicionarPapel(papeis.findByNome("ADMINISTRADOR").orElseThrow());
        usuarios.saveAndFlush(admin);
        var resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"vinculos@teste.local\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    @Test void apiCadastraLocalComVinculo() throws Exception {
        var modalidade = servico.criarModalidade("Sinuca", true);
        mvc.perform(post("/api/admin/configuracoes/locais").session(autenticar())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Mesa de Sinuca 1\",\"modalidadeIds\":[\"" + modalidade.getId() + "\"],\"ativo\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidadeIds[0]").value(modalidade.getId().toString()))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test void migracaoVinculaCampoExistenteAoFutebol() {
        var futebol = modalidades.findByNomeIgnoreCase("Futebol").orElseThrow();
        assertThat(locais.findByNomeIgnoreCase("Campo principal").orElseThrow().permiteModalidade(futebol.getId())).isTrue();
    }

    @Test void permiteMultiplasModalidadesEAlteracaoDosVinculos() {
        var dama = servico.criarModalidade("Dama", true);
        var xadrez = servico.criarModalidade("Xadrez", true);
        var mesa = servico.criarLocal("Mesa 1", null, Set.of(dama.getId(), xadrez.getId()), true);
        locais.flush();
        assertThat(servico.listarLocais().stream().filter(l -> l.id().equals(mesa.getId())).findFirst().orElseThrow().modalidadeIds())
                .containsExactlyInAnyOrder(dama.getId(), xadrez.getId());
        servico.editarLocal(mesa.getId(), "Mesa compartilhada", "Salão", Set.of(dama.getId()), false);
        assertThat(mesa.getAtivo()).isFalse();
        assertThat(mesa.permiteModalidade(xadrez.getId())).isFalse();
        assertThatThrownBy(() -> servico.excluirModalidade(dama.getId())).isInstanceOf(RegraNegocioException.class);
    }

    @Test void rejeitaVinculoInexistente() {
        assertThatThrownBy(() -> servico.criarLocal("Mesa inválida", null, Set.of(UUID.randomUUID()), true))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void apiExigeModalidadesNoCadastroDoLocal() throws Exception {
        mvc.perform(post("/api/admin/configuracoes/locais").session(autenticar()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Mesa sem modalidade\",\"modalidadeIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminListaInativosEOrganizadorApenasAtivos() throws Exception {
        var modalidade = servico.criarModalidade("Modalidade inativa", false);
        servico.criarLocal("Local inativo", null, Set.of(modalidade.getId()), false);
        var sessao = autenticar();
        mvc.perform(get("/api/admin/configuracoes/locais").session(sessao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.nome == 'Local inativo')].ativo").value(org.hamcrest.Matchers.contains(false)));
        mvc.perform(get("/api/organizador/partidas/locais").session(sessao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.nome == 'Local inativo')]").isEmpty());
    }
}
