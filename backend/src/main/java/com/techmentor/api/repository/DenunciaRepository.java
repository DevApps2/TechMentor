package com.techmentor.api.repository;

import com.techmentor.api.model.Denuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {
    List<Denuncia> findByDenuncianteIdOrderByCriadaEmDesc(Long denuncianteId);

    // Limite de denúncias: quantas esta pessoa fez desde o horário informado
    long countByDenuncianteIdAndCriadaEmAfter(Long denuncianteId, LocalDateTime desde);

    // Limite por alvo: esta pessoa já denunciou este usuário desde o horário informado?
    boolean existsByDenuncianteIdAndDenunciadoIdAndCriadaEmAfter(Long denuncianteId, Long denunciadoId, LocalDateTime desde);

    // Esta pessoa já denunciou este comentário de avaliação?
    boolean existsByDenuncianteIdAndAvaliacaoId(Long denuncianteId, Long avaliacaoId);
}
