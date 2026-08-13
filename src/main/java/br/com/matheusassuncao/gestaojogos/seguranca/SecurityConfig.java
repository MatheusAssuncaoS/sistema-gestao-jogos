package br.com.matheusassuncao.gestaojogos.seguranca;

import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

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
     * Sem isso, o Spring Security responde a uma sessão expirada chamando
     * sendError(), que depende da resolução de erro padrão do
     * container/Spring Boot (pode virar página HTML, dependendo do Accept
     * da requisição) — inconsistente com o resto da API, que sempre
     * devolve ProblemDetail em JSON. Escrever a resposta aqui deixa
     * determinístico, e testável via MockMvc sem depender de um servidor
     * real (MockMvc não simula o forward para /error que sendError() dispara).
     */
    @Bean
    public SessionInformationExpiredStrategy expiredSessionStrategy(ObjectMapper objectMapper) {
        return evento -> {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Sua sessão expirou. Entre novamente."
            );
            problema.setTitle("Sessão expirada");

            evento.getResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
            evento.getResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);
            // OutputStream, não getWriter(): o Writer usa a codificação da
            // resposta, que sem configuração explícita é ISO-8859-1 e corrompe
            // acento ("sessão" virava "sess?o"). Escrevendo no stream, o
            // ObjectMapper serializa em UTF-8 diretamente, do mesmo jeito que
            // o conversor JSON do Spring MVC já faz para toda outra resposta.
            objectMapper.writeValue(evento.getResponse().getOutputStream(), problema);
        };
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    SessionInformationExpiredStrategy expiredSessionStrategy)
            throws Exception {
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
                // sessionRegistry para poderem ser encerradas sob demanda.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry())
                        .expiredSessionStrategy(expiredSessionStrategy)
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
