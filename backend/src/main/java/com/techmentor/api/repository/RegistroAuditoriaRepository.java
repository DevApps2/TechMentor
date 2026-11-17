package com.techmentor.api.repository;

import com.techmentor.api.model.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    List<RegistroAuditoria> findByEntidadeAndEntidadeIdOrderByQuandoDescIdDesc(String entidade, Long entidadeId);
}
