package com.techmentor.api.service;

import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Modulo;
import com.techmentor.api.repository.ModuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModuloService {

    private final ModuloRepository moduloRepository;
    private final CursoService cursoService;



    public Modulo criarModulo(String tituloCurso, Modulo modulo) {
        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        if (moduloRepository.existsByCursoAndOrdem(curso, modulo.getOrdem())) {
            throw new ConflictException("Já existe um módulo com essa ordem no curso!");
        }

        modulo.setId(null);
        modulo.setCurso(curso);
        return moduloRepository.save(modulo);
    }

    public void deletarModulo(String tituloCurso, int ordem) {
        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Modulo modulo = moduloRepository.findByCursoAndOrdem(curso, ordem)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));

        moduloRepository.delete(modulo);
    }

    public Modulo atualizarModulo(String tituloCurso, int ordem, Modulo alterar) {
        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Modulo modulo = moduloRepository.findByCursoAndOrdem(curso, ordem)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));
        if(alterar.getOrdem() != ordem &&
        moduloRepository.existsByCursoAndOrdem(curso, alterar.getOrdem())) {
            throw new ConflictException("Já existe um módulo com essa ordem no curso!");
        }

        modulo.setTitulo(alterar.getTitulo());
        modulo.setOrdem(alterar.getOrdem());

        return moduloRepository.save(modulo);
    }



}