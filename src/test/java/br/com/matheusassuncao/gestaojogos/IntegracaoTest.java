package br.com.matheusassuncao.gestaojogos;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes de integração.
 *
 * Sobe um PostgreSQL real em container, o mesmo da produção, em vez de um
 * banco em memória. Isso garante que migrations, índices parciais e o
 * comportamento transacional sejam exercitados de verdade, e prepara o
 * terreno para os testes de concorrência das próximas issues.
 *
 * O container é estático: sobe uma vez e é reaproveitado por todos os testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegracaoTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    static {
        // O Docker Engine 29+ recusa a versão de API que o cliente do
        // Testcontainers negocia por padrão. Fixar a versão resolve.
        System.setProperty("api.version", "1.44");
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Limpa os dados transacionais antes de cada teste, mantendo os dados de
     * referência criados pelas migrations (papéis e categorias).
     *
     * A limpeza fica centralizada aqui porque a ordem de execução das classes
     * de teste não é garantida: cada uma precisa começar de um estado limpo,
     * independentemente de quem rodou antes. O CASCADE resolve as chaves
     * estrangeiras sem exigir que cada teste conheça a cadeia de dependências.
     */
    @BeforeEach
    void limparDadosTransacionais() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    token_recuperacao_senha,
                    jogador,
                    usuario_papel,
                    usuario
                CASCADE
                """);
    }
}