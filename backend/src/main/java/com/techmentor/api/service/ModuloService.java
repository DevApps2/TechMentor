package com.techmentor.api.service;

import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Modulo;
import com.techmentor.api.repository.ModuloRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ModuloService {

    public ModuloRepository moduloRepository;
    public ModuloService(ModuloRepository moduloRepository) {
        this.moduloRepository = moduloRepository;
    }

    public List<Modulo> listarModulos(){
        return this.moduloRepository.findAll();
    }

    public Modulo buscarModulo(String tituloCurso, int ordem) {
        return moduloRepository.findByCursoTituloIgnoreCaseAndOrdem(tituloCurso, ordem)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));
    }


}
