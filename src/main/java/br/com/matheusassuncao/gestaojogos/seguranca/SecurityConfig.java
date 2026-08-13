package br.com.matheusassuncao.gestaojogos.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Rastreia as sessões ativas de cada usuário, para que uma redefinição
     * de senha pelo administrador consiga encerrá-las (ver JogadorAtivoService).
     * HttpSessionEventPublisher é o que mantém o registro sincronizado
     * quando uma sessão expira ou é invalidada.
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Exposto como bean para que o AuthService consiga autenticar
     * as credenciais no endpoint de login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // DÍVIDA TÉCNICA: a autenticação por sessão usa cookie, que o navegador
                // envia automaticamente, então o CSRF é uma proteção legítima aqui.
                // Foi desabilitado para simplificar o MVP e o consumo por clientes REST.
                // A migração para JWT stateless elimina a necessidade dessa proteção.
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/cadastro").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/recuperacao-senha/**").permitAll()
                        .anyRequest().authenticated()
                )

                // sessão criada apenas quando há login efetivo. maximumSessions(-1)
                // não impõe teto de sessões simultâneas, só registra cada uma no
                // sessionRegistry para poderem ser encerradas sob demanda. Sem o
                // expiredSessionStrategy, uma sessão encerrada responde 200 com uma
                // mensagem em texto puro (comportamento padrão do Spring Security),
                // inconsistente com o resto da API — aqui vira 401, como qualquer
                // outra falta de autenticação.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry())
                        .expiredSessionStrategy(evento ->
                                evento.getResponse().sendError(HttpStatus.UNAUTHORIZED.value()))
                )

                // API REST responde 401 em vez de redirecionar para tela de login
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
