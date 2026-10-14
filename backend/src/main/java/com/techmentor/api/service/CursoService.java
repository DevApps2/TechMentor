package com.techmentor.api.service;

import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Curso;
import com.techmentor.api.repository.CursoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public List<Curso> listarCursos() {
        return cursoRepository.findAllByAtivoTrue();
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
            existente.setIconeUrl(curso.getIconeUrl());
            existente.setAtivo(true);
            return cursoRepository.save(existente);
        }

        return cursoRepository.save(curso);
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
        existente.setIconeUrl(curso.getIconeUrl());
        existente.setAtivo(curso.isAtivo());

        cursoRepository.save(existente);
    }

    public void deletarCurso(String titulo) {
        Curso existente = buscarPorTitulo(titulo);
        existente.setAtivo(false);
        cursoRepository.save(existente);
    }

}