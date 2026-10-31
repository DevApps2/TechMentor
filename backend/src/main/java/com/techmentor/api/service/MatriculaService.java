package com.techmentor.api.service;

import com.techmentor.api.dto.CursoDtos.MatriculaResponse;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Matricula;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.AulaRepository;
import com.techmentor.api.repository.MatriculaRepository;
import com.techmentor.api.repository.ProgressoAulaRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatriculaService {
    private final MatriculaRepository matriculaRepository;
    private final CursoService cursoService;
    private final UsuarioRepository usuarioRepository;
    private final AulaRepository aulaRepository;
    private final ProgressoAulaRepository progressoAulaRepository;

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
        return comProgresso(matricular(tituloCurso, usuarioId));
    }

    public List<Matricula> listarMatriculas(Long usuarioId){
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        return matriculaRepository.findByUsuario(usuario);
    }

    /** Cursos em andamento primeiro (mais recente no topo): o primeiro item é o "continue aprendendo". */
    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarMatriculasDTO(Long usuarioId) {
        return listarMatriculas(usuarioId).stream()
                .sorted(Comparator.comparing((Matricula m) -> m.getConcluidoEm() != null)
                        .thenComparing(Matricula::getMatriculadaEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::comProgresso)
                .toList();
    }

    private MatriculaResponse comProgresso(Matricula matricula) {
        long total = aulaRepository.countByModulo_Curso(matricula.getCurso());
        long concluidas = progressoAulaRepository.countByUsuarioAndAula_Modulo_Curso(
                matricula.getUsuario(), matricula.getCurso());
        return MatriculaResponse.from(matricula, concluidas, total);
    }
}
