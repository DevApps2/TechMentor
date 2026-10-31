package com.techmentor.api.repository;

import com.techmentor.api.model.Notificacao;
import com.techmentor.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByUsuarioOrderByCriadaEmDesc(Usuario usuario);
    List<Notificacao> findByUsuarioAndLidaOrderByCriadaEmDesc(Usuario usuario, boolean lida);
    Optional<Notificacao> findByIdAndUsuario(Long id, Usuario usuario);
}