package com.techmentor.api.repository;

import com.techmentor.api.model.Parceira;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParceiraRepository extends JpaRepository<Parceira, Long> {
    Optional<Parceira> findByUsuario(Usuario usuario);
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByDocumento(String documento);
    List<Parceira> findBySituacao(SituacaoParceira situacao);
}
