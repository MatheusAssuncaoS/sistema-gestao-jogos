package br.com.matheusassuncao.gestaojogos.config;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Papel;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

class AdminInicialBootstrapTest extends IntegracaoTest {

    @Autowired
    private AdminInicialBootstrap bootstrap;

    @Autowired
    private AdminInicialProperties propriedades;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void garantirAdministrador() {
        bootstrap.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("O administrador inicial é criado ATIVO e com o papel ADMINISTRADOR")
    void administradorInicialExiste() {
        Usuario admin = usuarioRepository
                .findByEmailIgnoreCase(propriedades.email())
                .orElseThrow(() -> new AssertionError(
                        "O administrador inicial deveria ter sido criado no boot."
                ));

        assertThat(admin.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(admin.getPapeis())
                .extracting(Papel::getNome)
                .containsExactly("ADMINISTRADOR");
    }

    @Test
    @DisplayName("Executar o bootstrap de novo não duplica o administrador")
    void bootstrapEIdempotente() {
        long antes = usuarioRepository.count();

        ApplicationArguments argumentos = new DefaultApplicationArguments();
        bootstrap.run(argumentos);
        bootstrap.run(argumentos);

        assertThat(usuarioRepository.count())
                .as("o bootstrap não deve criar usuários repetidos")
                .isEqualTo(antes);
    }

    @Test
    @DisplayName("A senha do administrador é persistida com hash")
    void senhaDoAdministradorTemHash() {
        Usuario admin = usuarioRepository
                .findByEmailIgnoreCase(propriedades.email())
                .orElseThrow();

        assertThat(admin.getSenhaHash()).isNotEqualTo(propriedades.senha());
    }
}
