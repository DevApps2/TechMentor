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
                        // Quem ainda não tem token: cadastrar, entrar e recuperar a senha
                        .requestMatchers(HttpMethod.POST,
                                "/usuarios",
                                "/usuarios/login",
                                "/usuarios/senha/esqueci",
                                "/usuarios/senha/redefinir"
                        ).permitAll()
                        // Fotos de perfil: o app carrega por URL (Coil), sem como mandar o cabeçalho
                        .requestMatchers(HttpMethod.GET, "/fotos/**").permitAll()
                        // Documentação
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/error").permitAll()
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

    private static void escreverErro(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"erro\": \"" + mensagem + "\"}");
    }
}