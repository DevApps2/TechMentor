package com.techmentor.api.repository;

import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {
    boolean existsByCursoAndOrdem(Curso curso, int ordem);
    Optional<Modulo> findByCursoTituloIgnoreCaseAndOrdem(String tituloCurso, int ordem);
    Optional<Modulo> findByCursoAndOrdem(Curso curso, int ordem);

    public interface moduloRepository extends JpaRepository<Modulo, Long> {
        boolean existsByCursoAndOrdem(Curso curso, int ordem);
        Optional<Modulo> findByCursoAndOrdem(Curso curso, int ordem);
    }

}
