package br.com.matheusassuncao.gestaojogos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais do administrador criado na primeira inicialização.
 *
 * Em desenvolvimento os valores vêm do application.yaml; em produção devem
 * ser sobrescritos por variáveis de ambiente
 * (GESTAO_JOGOS_ADMIN_INICIAL_EMAIL e GESTAO_JOGOS_ADMIN_INICIAL_SENHA).
 */
@ConfigurationProperties(prefix = "gestao-jogos.admin-inicial")
public record AdminInicialProperties(
        boolean habilitado,
        String nome,
        String email,
        String senha
) {
}
