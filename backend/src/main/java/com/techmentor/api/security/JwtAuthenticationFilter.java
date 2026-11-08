package com.techmentor.api.security;

import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioRepository usuarioRepository
    ) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            String subject = jwtService.extrairSubject(token);
            Long usuarioId = Long.valueOf(subject);

            Usuario usuario = usuarioRepository
                    .findByIdAndAtivoTrue(usuarioId)
                    .orElse(null);

            // Token de antes da última troca de senha não vale mais
            if (usuario != null && jwtService.extrairVersao(token) == usuario.versaoDoToken()) {

                var autoridade = new SimpleGrantedAuthority(
                        "ROLE_" + usuario.getTipo().name()
                );

                var autenticacao = new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        List.of(autoridade)
                );

                SecurityContext context =
                        SecurityContextHolder.createEmptyContext();

                context.setAuthentication(autenticacao);

                SecurityContextHolder.setContext(context);
            }

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}