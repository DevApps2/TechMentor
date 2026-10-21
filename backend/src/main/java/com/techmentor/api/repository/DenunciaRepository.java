package com.techmentor.api.repository;

import com.techmentor.api.model.Denuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {
    List<Denuncia> findByDenuncianteIdOrderByCriadaEmDesc(Long denuncianteId);
}
