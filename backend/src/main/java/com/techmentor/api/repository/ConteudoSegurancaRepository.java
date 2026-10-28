package com.techmentor.api.repository;

import com.techmentor.api.model.ConteudoSeguranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConteudoSegurancaRepository extends JpaRepository<ConteudoSeguranca, Long> {
    List<ConteudoSeguranca> findAllByOrderByTipoAscIdAsc();

    List<ConteudoSeguranca> findByTipoOrderByIdAsc(ConteudoSeguranca.Tipo tipo);
}
