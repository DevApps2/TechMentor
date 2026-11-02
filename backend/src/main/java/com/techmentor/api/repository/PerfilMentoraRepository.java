package com.techmentor.api.repository;

import com.techmentor.api.model.PerfilMentora;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PerfilMentoraRepository extends JpaRepository<PerfilMentora, Long> {

    // O perfil de mentora de um usuário (cada usuário tem no máximo um)
    Optional<PerfilMentora> findByUsuarioId(Long usuarioId);

    // Para atualizar a nota média: a trava impede que duas avaliações simultâneas se sobrescrevam
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PerfilMentora p where p.usuario.id = :usuarioId")
    Optional<PerfilMentora> findByUsuarioIdForUpdate(@Param("usuarioId") Long usuarioId);
}