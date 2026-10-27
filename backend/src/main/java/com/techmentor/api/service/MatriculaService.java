package com.techmentor.api.service;

import com.techmentor.api.dto.CursoDtos.MatriculaResponse;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Matricula;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.MatriculaRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatriculaService {
    private final MatriculaRepository matriculaRepository;
    private final CursoService cursoService;
    private final UsuarioRepository usuarioRepository;

    public Matricula matricular(String tituloCurso, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        if (matriculaRepository.existsByUsuarioAndCurso(usuario, curso)) {
            throw new ConflictException("Usuário já cadastrado nesse curso!");
        }

        Matricula matricula = new Matricula();
        matricula.setUsuario(usuario);
        matricula.setCurso(curso);
        return matriculaRepository.save(matricula);
    }

    @Transactional
    public MatriculaResponse matricularDTO(String tituloCurso, Long usuarioId) {
        return MatriculaResponse.from(matricular(tituloCurso, usuarioId));
    }

    public List<Matricula> listarMatriculas(Long usuarioId){
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        return matriculaRepository.findByUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarMatriculasDTO(Long usuarioId) {
        return listarMatriculas(usuarioId).stream().map(MatriculaResponse::from).toList();
    }
}
