package com.techmentor.api.service;

import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Descobre quem está logado a partir do token JWT. O cliente não escolhe o id. */
@Service
@Transactional(readOnly = true)
public class UsuarioAtualService {

    private final UsuarioRepository usuarios;

    public UsuarioAtualService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario buscarAtual() {
        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || !(autenticacao.getPrincipal() instanceof Usuario usuario)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }

        // Recarrega do banco: se a conta foi desativada depois do login, deixa de valer na hora
        return usuarios.findByIdAndAtivoTrue(usuario.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Autenticação necessária."));
    }
}