package com.techmentor.api.repository;

import com.techmentor.api.model.BloqueioUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BloqueioUsuarioRepository extends JpaRepository<BloqueioUsuario, Long> {
    List<BloqueioUsuario> findByBloqueadorIdOrderByCriadoEmDesc(Long bloqueadorId);
    Optional<BloqueioUsuario> findByBloqueadorIdAndBloqueadoId(Long bloqueadorId, Long bloqueadoId);
    boolean existsByBloqueadorIdAndBloqueadoId(Long bloqueadorId, Long bloqueadoId);
    boolean existsByBloqueadorIdAndBloqueadoIdOrBloqueadorIdAndBloqueadoId(Long a, Long b, Long c, Long d);
}
