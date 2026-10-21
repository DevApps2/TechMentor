package com.techmentor.api.service;

import com.techmentor.api.ApiException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolve o usuário das rotas do módulo sem substituir o serviço de autenticação compartilhado. */
@Service
@Transactional(readOnly = true)
public class UsuarioAtualService {
    private final UsuarioRepository usuarios;

    public UsuarioAtualService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    public Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    }
}
