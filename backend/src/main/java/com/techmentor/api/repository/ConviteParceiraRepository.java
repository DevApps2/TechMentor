package com.techmentor.api.repository;

import com.techmentor.api.model.ConviteParceira;
import com.techmentor.api.model.Parceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConviteParceiraRepository extends JpaRepository<ConviteParceira, Long> {
    Optional<ConviteParceira> findFirstByParceiraAndUsadoFalseOrderByCriadoEmDesc(Parceira parceira);
    List<ConviteParceira> findByParceiraAndUsadoFalse(Parceira parceira);
    boolean existsByParceiraAndCriadoEmAfter(Parceira parceira, LocalDateTime desde);
}
