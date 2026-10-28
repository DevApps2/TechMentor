package com.techmentor.api.service;

import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Curso;
import com.techmentor.api.repository.AulaRepository;
import com.techmentor.api.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final AulaRepository aulaRepository;

    public List<Curso> listarCursosAtivos() {
        return cursoRepository.findAllByAtivoTrue();
    }

    public List<Curso> listarCursosAtivos(Curso.Categoria categoria) {
        return categoria == null ? listarCursosAtivos() : cursoRepository.findByAtivoTrueAndCategoria(categoria);
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> listarCursosDTO(Curso.Categoria categoria) {
        List<Curso> cursos = listarCursosAtivos(categoria);
        cursos.forEach(curso -> curso.setTotalAulas(aulaRepository.countByModulo_Curso(curso)));
        return cursos.stream().map(CursoResponse::from).toList();
    }

    public Curso detalharCurso(String titulo) {
        Curso curso = buscarCursoAtivo(titulo);
        curso.setTotalAulas(aulaRepository.countByModulo_Curso(curso));
        return curso;
    }

    @Transactional(readOnly = true)
    public CursoResponse detalharCursoDTO(String titulo) {
        return CursoResponse.from(detalharCurso(titulo));
    }

    public Curso buscarCursoAtivo(String titulo) {
        return cursoRepository.findByTituloIgnoreCase(titulo)
                .filter(Curso::isAtivo)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado!"));
    }
    public List<Curso> listarCursosDesativados(){
        return cursoRepository.findAllByAtivoFalse();
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> listarCursosDesativadosDTO() {
        List<Curso> cursos = listarCursosDesativados();
        cursos.forEach(curso -> curso.setTotalAulas(aulaRepository.countByModulo_Curso(curso)));
        return cursos.stream().map(CursoResponse::from).toList();
    }
    public Curso buscarPorTitulo(String titulo) {
        return cursoRepository.findByTituloIgnoreCase(titulo)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado!"));
    }

    public Curso criarCurso(Curso curso) {
        Optional<Curso> encontrado = cursoRepository.findByTituloIgnoreCase(curso.getTitulo());

        if (encontrado.isPresent()) {
            Curso existente = encontrado.get();
            if (existente.isAtivo()) {
                throw new ConflictException("Curso já cadastrado!");
            }
            existente.setDescricao(curso.getDescricao());
            existente.setCategoria(curso.getCategoria());
            existente.setNivel(curso.getNivel());
            existente.setIconeUrl(curso.getIconeUrl());
            existente.setAtivo(true);
            return cursoRepository.save(existente);
        }

        return cursoRepository.save(curso);
    }

    @Transactional
    public CursoResponse criarCurso(CursoRequest request) {
        Curso salvo = criarCurso(request.toEntity());
        salvo.setTotalAulas(aulaRepository.countByModulo_Curso(salvo));
        return CursoResponse.from(salvo);
    }

    public void atualizarCurso(String tituloAtual, Curso curso) {
        Curso existente = buscarPorTitulo(tituloAtual);

        boolean mudouTitulo = !existente.getTitulo().equalsIgnoreCase(curso.getTitulo());
        if (mudouTitulo && cursoRepository.existsByTituloIgnoreCase(curso.getTitulo())) {
            throw new ConflictException("Curso já cadastrado!");
        }

        existente.setTitulo(curso.getTitulo());
        existente.setDescricao(curso.getDescricao());
        existente.setCategoria(curso.getCategoria());
        existente.setNivel(curso.getNivel());
        existente.setIconeUrl(curso.getIconeUrl());
        existente.setAtivo(curso.isAtivo());

        cursoRepository.save(existente);
    }

    @Transactional
    public CursoResponse atualizarCurso(String tituloAtual, CursoRequest request) {
        Curso curso = request.toEntity();
        atualizarCurso(tituloAtual, curso);
        Curso salvo = buscarPorTitulo(curso.getTitulo());
        salvo.setTotalAulas(aulaRepository.countByModulo_Curso(salvo));
        return CursoResponse.from(salvo);
    }

    public void deletarCurso(String titulo) {
        Curso existente = buscarPorTitulo(titulo);
        existente.setAtivo(false);
        cursoRepository.save(existente);
    }

}
