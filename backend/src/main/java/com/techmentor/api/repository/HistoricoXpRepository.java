package com.techmentor.api.repository;

import com.techmentor.api.model.HistoricoXp;
import com.techmentor.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoXpRepository extends JpaRepository<HistoricoXp, Long> {
    List<HistoricoXp> findByUsuarioOrderByCriadoEmDesc(Usuario usuario);
}
