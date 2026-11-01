package com.techmentor.api.repository;

import com.techmentor.api.model.PerfilMentora;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilMentoraRepository extends JpaRepository<PerfilMentora, Long> {

    // O perfil de mentora de um usuário (cada usuário tem no máximo um)
    Optional<PerfilMentora> findByUsuarioId(Long usuarioId);
}