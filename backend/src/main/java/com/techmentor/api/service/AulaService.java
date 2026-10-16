package com.techmentor.api.service;

import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Aula;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Modulo;
import com.techmentor.api.repository.AulaRepository;
import com.techmentor.api.repository.ModuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AulaService {

    private final ModuloRepository moduloRepository;
    private final AulaRepository aulaRepository;
    private final CursoService cursoService;

    public Aula CriarAula (String tituloCurso,int ordemModulo, Aula aula){
        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);
        Modulo modulo = moduloRepository.findByCursoAndOrdem(curso, ordemModulo)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));

        if(aulaRepository.existsByModuloAndOrdem(modulo, aula.getOrdem())) {
            throw new ConflictException("Já existe uma aula com essa ordem neste módulo");
        }

        aula.setId(null);
        aula.setModulo(modulo);
        return aulaRepository.save(aula);
    }

    public void deletarAula(String tituloCurso, int ordemModulo, int ordemAula) {
        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Modulo modulo = moduloRepository.findByCursoAndOrdem(curso, ordemModulo)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));

        Aula aula = aulaRepository.findByModuloAndOrdem(modulo, ordemAula)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada!"));

        aulaRepository.delete(aula);
    }

    public Aula atualizarAula(String tituloCurso, int ordemModulo, int ordemAula, Aula dados) {
        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Modulo modulo = moduloRepository.findByCursoAndOrdem(curso, ordemModulo)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));

        Aula aula = aulaRepository.findByModuloAndOrdem(modulo, ordemAula)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada!"));

        if (dados.getOrdem() != ordemAula && aulaRepository.existsByModuloAndOrdem(modulo, dados.getOrdem())) {
            throw new ConflictException("Já existe uma aula com essa ordem neste módulo!");
        }

        aula.setTitulo(dados.getTitulo());
        aula.setOrdem(dados.getOrdem());
        aula.setDuracaoMinutos(dados.getDuracaoMinutos());
        aula.setXpRecompensa(dados.getXpRecompensa());
        aula.setConteudoUrl(dados.getConteudoUrl());
        return aulaRepository.save(aula);
    }

}
