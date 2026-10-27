package com.techmentor.api.service;

import com.techmentor.api.dto.UsuarioDtos.CreateRequest;
import com.techmentor.api.dto.UsuarioDtos.Response;
import com.techmentor.api.dto.UsuarioDtos.UpdateRequest;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotAuthorizedException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public List<Response> listarUsuarios() {
        return repository.findAll()
                .stream()
                .map(Response::from)
                .toList();
    }

    public Response criarUsuario(CreateRequest dto) {

        if (repository.existsByEmail(dto.email())) {
            throw new ConflictException("Usuário já cadastrado!");
        }

        Usuario usuario = new Usuario();

        usuario.setId(null);
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));

        Usuario usuarioSalvo = repository.save(usuario);

        return Response.from(usuarioSalvo);
    }

    @Transactional
    public void atualizarUsuario(Long id, UpdateRequest dto) {

        Usuario existente = repository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Usuário não encontrado!"));

        boolean mudouEmail =
                !existente.getEmail().equals(dto.email());

        if (mudouEmail && repository.existsByEmail(dto.email())) {
            throw new ConflictException("E-mail já cadastrado!");
        }

        existente.setNome(dto.nome());
        existente.setEmail(dto.email());
        existente.setFotoUrl(dto.fotoUrl());
    }

    public Usuario loginUsuario(String email, String senha) {

        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() ->
                        new NotAuthorizedException("Credenciais inválidas!"));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new NotAuthorizedException("Credenciais inválidas!");
        }

        if (!usuario.isAtivo()) {
            throw new NotAuthorizedException("Credenciais inválidas!");
        }

        return usuario;
    }

    public void desativarUsuario(Long id) {

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        if(!usuario.isAtivo()){
            throw new ConflictException("Usuário já desativado!");
        }

        usuario.setAtivo(false);
        repository.save(usuario);
    }
}
