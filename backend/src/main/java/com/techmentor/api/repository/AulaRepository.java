package com.techmentor.api.repository;

import com.techmentor.api.model.Aula;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    boolean existsByModuloAndOrdem(Modulo modulo, int ordem);

    Long countByModulo_Curso(Curso curso);
    Optional<Aula> findByModuloAndOrdem(Modulo modulo, int ordem);

    /** Todas as aulas do curso na ordem de estudo (módulo e depois aula). */
    List<Aula> findByModulo_CursoOrderByModulo_OrdemAscOrdemAsc(Curso curso);

}
