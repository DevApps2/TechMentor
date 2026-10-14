package com.techmentor.api.repository;

import com.techmentor.api.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findAllByAtivoTrue();

    boolean existsByTituloIgnoreCase(String titulo);
    Optional<Curso> findByTituloIgnoreCase(String titulo);
}