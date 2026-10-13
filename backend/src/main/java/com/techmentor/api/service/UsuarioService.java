package com.techmentor.api.service;

import com.techmentor.api.execptions.ConflictExecption;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public List<Usuario> listarUsuarios(){
        return repository.findAll();
    }

    public Optional<Usuario> criarUsuario(Usuario usuario) {
        if(repository.existsByEmail(usuario.getEmail())) {
            throw new ConflictExecption("Usuário já cadastrado!");
        }
        return Optional.of(this.repository.save(usuario));
    }

}
