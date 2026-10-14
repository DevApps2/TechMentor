package com.techmentor.api.service;

import com.techmentor.api.dto.UsuarioResponseDTO;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotAuthorizedException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponseDTO> listarUsuarios(){
        return repository.findAll().stream()
                .map(UsuarioResponseDTO::from)
                .toList();
    }

    public Optional<Usuario> criarUsuario(Usuario usuario) {
        if (repository.existsByEmail(usuario.getEmail())) {
            throw new ConflictException("Usuário já cadastrado!");
        }
        // PROVISÓRIO: por enquanto a senha chega no campo senhaHash
        usuario.setSenhaHash(passwordEncoder.encode(usuario.getSenhaHash()));
        return Optional.of(repository.save(usuario));
    }


    public void atualizarUsuario(long id, Usuario usuario) {
        Usuario existente = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        boolean mudouEmail = !existente.getEmail().equals(usuario.getEmail());
        if (mudouEmail && repository.existsByEmail(usuario.getEmail())) {
            throw new ConflictException("E-mail já cadastrado!");
        }

        existente.setNome(usuario.getNome());
        existente.setEmail(usuario.getEmail());
        existente.setFotoUrl(usuario.getFotoUrl());
        repository.save(existente);
    }

    public Usuario loginUsuario(String email, String senha) {
        Usuario usuario = (Usuario) repository.findByEmail(email)
                .orElseThrow(() -> new NotAuthorizedException("Credenciais inválidas!"));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new NotAuthorizedException("Credenciais inválidas!");
        }
        return usuario;
    }

    public void deletarUsuario(long id){
        if(!repository.existsById(id)) {
            throw new NotFoundException("Usuário não encontrado!");
        }
        repository.deleteById(id);
    }
}
