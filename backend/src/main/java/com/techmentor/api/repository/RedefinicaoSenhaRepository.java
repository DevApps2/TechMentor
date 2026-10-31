package com.techmentor.api.repository;

import com.techmentor.api.model.RedefinicaoSenha;
import com.techmentor.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RedefinicaoSenhaRepository extends JpaRepository<RedefinicaoSenha, Long> {

    // O pedido mais recente que ainda pode ser usado: serve para validar o código digitado
    Optional<RedefinicaoSenha> findFirstByUsuarioAndUsadoFalseOrderByCriadoEmDesc(Usuario usuario);

    // Usado para o limite de pedidos: "esta pessoa já pediu um código no último minuto?"
    boolean existsByUsuarioAndCriadoEmAfter(Usuario usuario, LocalDateTime desde);

    // Para invalidar pedidos antigos quando chega um novo
    List<RedefinicaoSenha> findByUsuarioAndUsadoFalse(Usuario usuario);
}