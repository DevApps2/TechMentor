package com.techmentor.api.config;

import com.techmentor.api.exceptions.ErroApi;
import com.techmentor.api.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import com.techmentor.api.model.Usuario;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Sem token (ou token inválido/vencido): 401. Token válido mas sem permissão: 403.
                // Os dois no mesmo formato do resto da API (ErroApi: erro + codigo)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) ->
                                escreverErro(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária."))
                        .accessDeniedHandler((request, response, ex) ->
                                escreverErro(response, HttpStatus.FORBIDDEN, "Acesso não permitido."))
                )

                .authorizeHttpRequests(auth -> auth
                        // 1. Públicas: quem ainda não tem token, fotos (o app carrega por URL), documentação e página de erro
                        .requestMatchers(HttpMethod.POST,
                                "/usuarios", "/usuarios/login", "/usuarios/senha/esqueci", "/usuarios/senha/redefinir",
                                "/parceiras/convite/aceitar"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/fotos/**", "/parceiras/termos").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()

                        // 2. Só administradoras (módulos e aulas só têm rotas de gestão: vale para qualquer método)
                        .requestMatchers("/api/admin/**", "/modulos/**", "/aulas/**").hasRole("ADMIN")

                        // 2b. Só empresas parceiras: a área delas
                        .requestMatchers("/api/parceira/**").hasRole("PARCEIRA")
                        .requestMatchers(HttpMethod.POST, "/cursos", "/conquistas", "/desafios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/cursos/**", "/conquistas/**", "/desafios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/cursos/**", "/conquistas/**", "/desafios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/usuarios", "/cursos/desativados").hasRole("ADMIN")

                        // 3. Dados pessoais: só a própria usuária (nem a admin). As notificações servem a todas, parceira inclusa
                        .requestMatchers(
                                "/notificacoes/usuarios/{usuarioId}",
                                "/notificacoes/leitura/usuarios/{usuarioId}"
                        ).access(proprioUsuario("usuarioId", false))

                        // 3b. O restante dos dados pessoais é do app das alunas e mentoras: a parceira não entra
                        .requestMatchers(
                                "/matriculas/usuarios/{usuarioId}",
                                "/matriculas/{curso}/usuarios/{usuarioId}",
                                "/progresso/{titulo}/usuarios/{usuarioId}",
                                "/progresso/{titulo}/usuarios/{usuarioId}/aulas",
                                "/progresso/{titulo}/{ordemModulo}/{ordemAula}/usuarios/{usuarioId}",
                                "/historico-xp/usuarios/{usuarioId}",
                                "/conquistas/usuarios/{usuarioId}",
                                "/desafios/usuarios/{usuarioId}",
                                "/desafios/{titulo}/participacao/usuarios/{usuarioId}"
                        ).access(proprioUsuario("usuarioId", false, true))

                        // 4. Conta: trocar a senha é só da dona; editar, mandar foto e desativar, a dona ou a admin
                        .requestMatchers(HttpMethod.GET, "/usuarios/busca").access(todasMenosParceira())
                        .requestMatchers(HttpMethod.GET, "/usuarios/{id}").access(proprioUsuario("id", true))
                        .requestMatchers(HttpMethod.PATCH, "/usuarios/{id}/senha").access(proprioUsuario("id", false))
                        .requestMatchers(HttpMethod.PUT, "/usuarios/{id}").access(proprioUsuario("id", true))
                        .requestMatchers(HttpMethod.POST, "/usuarios/{id}/foto").access(proprioUsuario("id", true))
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/{id}").access(proprioUsuario("id", true))

                        // Todo o resto (o app das alunas e mentoras): basta estar logada, menos a parceira, que só tem a área dela
                        .anyRequest().access(todasMenosParceira())
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /** O título do curso vai na URL e pode ter "/" (UX%2FUI). O Tomcat já aceita; o firewall do Spring Security também precisa aceitar. */
    @Bean
    public WebSecurityCustomizer liberarBarraCodificadaNaUrl() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedPercent(true);   // com o Tomcat em "passthrough", o %2F chega com o "%" no caminho
        return web -> web.httpFirewall(firewall);
    }

    /**
     * Logada e que não seja parceira. A empresa parceira só usa /api/parceira, a própria conta, as notificações e as fotos:
     * sem esta trava ela alcançaria matrículas, mentorias, denúncias e o resto do app das alunas.
     */
    private static AuthorizationManager<RequestAuthorizationContext> todasMenosParceira() {
        return (autenticacao, contexto) -> {
            Authentication auth = autenticacao.get();
            boolean permitido = auth != null
                    && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof Usuario usuario
                    && usuario.getTipo() != Usuario.Tipo.PARCEIRA;
            return new AuthorizationDecision(permitido);
        };
    }

    /**
     * Libera só se o id que está no caminho da URL (por exemplo {usuarioId}) for o da pessoa logada.
     * Com adminTambem = true, a administradora também passa.
     */
    private static AuthorizationManager<RequestAuthorizationContext> proprioUsuario(String variavel, boolean adminTambem) {
        return proprioUsuario(variavel, adminTambem, false);
    }

    /** Com menosParceira = true, a empresa parceira não passa nem no próprio id (são dados do app das alunas). */
    private static AuthorizationManager<RequestAuthorizationContext> proprioUsuario(String variavel, boolean adminTambem,
                                                                                    boolean menosParceira) {
        return (autenticacao, contexto) -> {
            Authentication auth = autenticacao.get();
            boolean permitido = auth != null
                    && auth.getPrincipal() instanceof Usuario usuario
                    && !(menosParceira && usuario.getTipo() == Usuario.Tipo.PARCEIRA)
                    && (String.valueOf(usuario.getId()).equals(contexto.getVariables().get(variavel))
                    || (adminTambem && usuario.getTipo() == Usuario.Tipo.ADMIN));
            return new AuthorizationDecision(permitido);
        };
    }

    /**
     * O login é feito por UsuarioService + JWT, não pelo mecanismo de usuário/senha do Spring Security.
     * Declarar este serviço (que nunca acha ninguém) impede o Spring de criar um usuário padrão com senha
     * gerada, e tira o aviso "Using generated security password" do console.
     */
    @Bean
    public UserDetailsService semUsuariosDoSpring() {
        return username -> {
            throw new UsernameNotFoundException("O login é feito pelo UsuarioService, com JWT.");
        };
    }

    private static void escreverErro(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(ErroApi.de(status, mensagem).paraJson());
    }
}