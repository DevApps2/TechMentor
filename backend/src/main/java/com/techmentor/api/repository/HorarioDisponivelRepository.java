package com.techmentor.api.repository;

import com.techmentor.api.model.HorarioDisponivel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HorarioDisponivelRepository extends JpaRepository<HorarioDisponivel, Long> {
    List<HorarioDisponivel> findByMentoraIdAndReservadoFalseOrderByInicio(Long mentoraId);
    List<HorarioDisponivel> findByMentoraId(Long mentoraId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HorarioDisponivel h where h.id = :id")
    Optional<HorarioDisponivel> findByIdForUpdate(@Param("id") Long id);
}
