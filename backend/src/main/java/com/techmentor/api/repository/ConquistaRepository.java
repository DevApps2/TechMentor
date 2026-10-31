package com.techmentor.api.repository;

import com.techmentor.api.model.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConquistaRepository extends JpaRepository<Conquista, Long> {
    boolean existsByNomeIgnoreCase(String nome);
    Optional<Conquista> findByNomeIgnoreCase(String nome);
}