package com.techmentor.api.repository;

import com.techmentor.api.model.Desafio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DesafioRepository extends JpaRepository<Desafio, Long> {
    boolean existsByTituloIgnoreCase(String titulo);
    Optional<Desafio> findByTituloIgnoreCase(String titulo);
    List<Desafio> findByInicioLessThanEqualAndFimGreaterThanEqual(LocalDate hoje, LocalDate hoje2);
}