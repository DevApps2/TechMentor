package com.techmentor.api.config;

import com.techmentor.api.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                // Os dois no mesmo formato do resto da API: {"erro": "..."}
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) ->
                                escreverErro(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária."))
                        .accessDeniedHandler((request, response, ex) ->
                                escreverErro(response, HttpStatus.FORBIDDEN, "Acesso não permitido."))
                )

                .authorizeHttpRequests(auth -> auth
                        // 1. Públicas: quem ainda não tem token, fotos (o app carrega por URL), documentação e página de erro
                        .requestMatchers(HttpMethod.POST,
                                "/usuarios", "/usuarios/login", "/usuarios/senha/esqueci", "/usuarios/senha/redefinir"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/fotos/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()

                        // 2. Só administradoras (módulos e aulas só têm rotas de gestão: vale para qualquer método)
                        .requestMatchers("/api/admin/**", "/modulos/**", "/aulas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/cursos", "/conquistas", "/desafios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/cursos/**", "/conquistas/**", "/desafios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/cursos/**", "/conquistas/**", "/desafios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/usuarios", "/cursos/desativados").hasRole("ADMIN")

                        // 3. Dados pessoais: só a própria usuária (nem a admin)
                        .requestMatchers(
                                "/notificacoes/usuarios/{usuarioId}",
                                "/notificacoes/leitura/usuarios/{usuarioId}",
                                "/matriculas/usuarios/{usuarioId}",
                                "/matriculas/{curso}/usuarios/{usuarioId}",
                                "/progresso/{titulo}/usuarios/{usuarioId}",
                                "/progresso/{titulo}/usuarios/{usuarioId}/aulas",
                                "/progresso/{titulo}/{ordemModulo}/{ordemAula}/usuarios/{usuarioId}",
                                "/historico-xp/usuarios/{usuarioId}",
                                "/conquistas/usuarios/{usuarioId}",
                                "/desafios/usuarios/{usuarioId}",
                                "/desafios/{titulo}/participacao/usuarios/{usuarioId}"
                        ).access(proprioUsuario("usuarioId", false))

                        // 4. Conta: trocar a senha é só da dona; editar, mandar foto e desativar, a dona ou a admin
                        .requestMatchers(HttpMethod.PATCH, "/usuarios/{id}/senha").access(proprioUsuario("id", false))
                        .requestMatchers(HttpMethod.PUT, "/usuarios/{id}").access(proprioUsuario("id", true))
                        .requestMatchers(HttpMethod.POST, "/usuarios/{id}/foto").access(proprioUsuario("id", true))
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/{id}").access(proprioUsuario("id", true))

                        // Todo o resto: basta estar logada
                        .anyRequest().authenticated()
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
     * Libera só se o id que está no caminho da URL (por exemplo {usuarioId}) for o da pessoa logada.
     * Com adminTambem = true, a administradora também passa.
     */
    private static AuthorizationManager<RequestAuthorizationContext> proprioUsuario(String variavel, boolean adminTambem) {
        return (autenticacao, contexto) -> {
            Authentication auth = autenticacao.get();
            boolean permitido = auth != null
                    && auth.getPrincipal() instanceof Usuario usuario
                    && (String.valueOf(usuario.getId()).equals(contexto.getVariables().get(variavel))
                    || (adminTambem && usuario.getTipo() == Usuario.Tipo.ADMIN));
            return new AuthorizationDecision(permitido);
        };
    }

    private static void escreverErro(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"erro\": \"" + mensagem + "\"}");
    }
}