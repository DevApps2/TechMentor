package com.techmentor.api.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.techmentor.api.model.Modulo;
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

    /** Quantas alunas diferentes concluíram esta aula. */
    @Query("select count(distinct p.usuario.id) from ProgressoAula p where p.aula = :aula")
    long alunasDaAula(@Param("aula") Aula aula);

    /** Quantas alunas diferentes concluíram alguma aula deste módulo. */
    @Query("select count(distinct p.usuario.id) from ProgressoAula p where p.aula.modulo = :modulo")
    long alunasDoModulo(@Param("modulo") Modulo modulo);

    /** Quantas alunas diferentes concluíram alguma aula deste curso. */
    @Query("select count(distinct p.usuario.id) from ProgressoAula p where p.aula.modulo.curso = :curso")
    long alunasDoCurso(@Param("curso") Curso curso);

    /** [id da aula, quantas alunas a concluíram] para as aulas do curso que alguém concluiu. */
    @Query("select p.aula.id, count(distinct p.usuario.id) from ProgressoAula p "
            + "where p.aula.modulo.curso = :curso group by p.aula.id")
    List<Object[]> concluintesPorAula(@Param("curso") Curso curso);

}
