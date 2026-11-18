package com.techmentor.api.repository;

import com.techmentor.api.model.PedidoParceria;
import com.techmentor.api.model.SituacaoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoParceriaRepository extends JpaRepository<PedidoParceria, Long> {
    List<PedidoParceria> findAllByOrderByCriadoEmDesc();
    List<PedidoParceria> findBySituacaoOrderByCriadoEmDesc(SituacaoPedido situacao);
    boolean existsByEmailIgnoreCaseAndSituacao(String email, SituacaoPedido situacao);
    boolean existsByDocumentoAndSituacao(String documento, SituacaoPedido situacao);
}
