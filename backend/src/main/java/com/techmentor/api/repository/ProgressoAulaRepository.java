package com.techmentor.api.repository;

import com.techmentor.api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProgressoAulaRepository extends JpaRepository<ProgressoAula, Long> {

    Long countByUsuarioAndAula_Modulo_Curso(Usuario usuario, Curso curso);

    boolean existsByUsuarioAndAula(Usuario usuario, Aula aula);

    List<ProgressoAula> findByUsuarioAndAula_Modulo_Curso(Usuario usuario, Curso curso);

    long countByUsuarioAndConcluidaEmBetween(Usuario usuario, LocalDateTime de, LocalDateTime ate);

}
