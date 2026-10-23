package com.techmentor.api.repository;

import com.techmentor.api.model.InscricaoOportunidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InscricaoOportunidadeRepository extends JpaRepository<InscricaoOportunidade, Long> {
    Optional<InscricaoOportunidade> findByUsuarioIdAndOportunidadeId(Long usuarioId, Long oportunidadeId);
    List<InscricaoOportunidade> findByUsuarioIdOrderByCriadaEmDesc(Long usuarioId);
}
