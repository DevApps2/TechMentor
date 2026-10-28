package com.techmentor.api.service;

import com.techmentor.api.dto.UsuarioDtos.CreateRequest;
import com.techmentor.api.dto.UsuarioDtos.Response;
import com.techmentor.api.dto.UsuarioDtos.Resumo;
import com.techmentor.api.dto.UsuarioDtos.UpdateRequest;
import com.techmentor.api.exceptions.ApiException;
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
        return repository.findByAtivoTrue()
                .stream()
                .map(Response::from)
                .toList();
    }

    /** Busca usuários ativos pelo nome (para denunciar ou bloquear); no máximo 20, sem o próprio solicitante. */
    public List<Resumo> buscarPorNome(String nome, Long solicitanteId) {
        String termo = nome == null ? "" : nome.trim();
        if (termo.length() < 2) {
            throw ApiException.badRequest("Digite pelo menos 2 letras para buscar.");
        }
        return repository.findTop20ByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(termo).stream()
                .filter(u -> !u.getId().equals(solicitanteId))
                .map(Resumo::from)
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

    public Response buscarPorId(Long id) {
        return Response.from(repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!")));
    }

    @Transactional
    public Response atualizarUsuario(Long id, UpdateRequest dto) {

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
        if (dto.bio() != null) {
            existente.setBio(dto.bio());
        }
        if (dto.areasInteresse() != null) {
            existente.getAreasInteresse().clear();
            dto.areasInteresse().stream()
                    .map(String::trim)
                    .filter(area -> !area.isEmpty())
                    .forEach(existente.getAreasInteresse()::add);
        }

        return Response.from(existente);
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
