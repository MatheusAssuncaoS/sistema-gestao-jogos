package br.com.matheusassuncao.gestaojogos.config;

import br.com.matheusassuncao.gestaojogos.dominio.Papel;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve o problema de inicialização: sem um administrador, ninguém
 * conseguiria aprovar o primeiro cadastro (UC27).
 *
 * Roda a cada boot, mas só cria o usuário se ele ainda não existir, então
 * é seguro executar repetidamente.
 */
@Component
public class AdminInicialBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialBootstrap.class);

    private final AdminInicialProperties propriedades;
    private final UsuarioRepository usuarioRepository;
    private final PapelRepository papelRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInicialBootstrap(AdminInicialProperties propriedades,
                                 UsuarioRepository usuarioRepository,
                                 PapelRepository papelRepository,
                                 PasswordEncoder passwordEncoder) {
        this.propriedades = propriedades;
        this.usuarioRepository = usuarioRepository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!propriedades.habilitado()) {
            return;
        }

        if (usuarioRepository.existsByEmailIgnoreCase(propriedades.email())) {
            return;
        }

        Papel administrador = papelRepository.findByNome("ADMINISTRADOR")
                .orElseThrow(() -> new IllegalStateException(
                        "Papel ADMINISTRADOR não encontrado. Verifique a migration V1."
                ));

        Usuario usuario = new Usuario(
                propriedades.nome(),
                propriedades.email().toLowerCase(),
                passwordEncoder.encode(propriedades.senha())
        );

        usuario.ativar();
        usuario.adicionarPapel(administrador);

        usuarioRepository.save(usuario);

        log.warn("Administrador inicial criado com o e-mail {}. Troque a senha padrão.",
                propriedades.email());
    }
}
