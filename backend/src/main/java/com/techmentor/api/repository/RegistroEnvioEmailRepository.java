package com.techmentor.api.repository;

import com.techmentor.api.model.RegistroEnvioEmail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroEnvioEmailRepository extends JpaRepository<RegistroEnvioEmail, Long> {

    boolean existsByChaveEvento(String chaveEvento);

    @Query("select r from RegistroEnvioEmail r where r.situacao in :situacoes " +
            "and r.proximaTentativaEm <= :agora and r.textoRetentativa is not null order by r.proximaTentativaEm")
    List<RegistroEnvioEmail> encontrarRetentativas(
            @Param("situacoes") List<RegistroEnvioEmail.Situacao> situacoes,
            @Param("agora") LocalDateTime agora,
            Pageable pagina);

    @Modifying
    @Transactional
    @Query("update RegistroEnvioEmail r set r.situacao = :nova where r.id = :id and r.situacao = :anterior")
    int reservarRetentativa(
            @Param("id") Long id,
            @Param("anterior") RegistroEnvioEmail.Situacao anterior,
            @Param("nova") RegistroEnvioEmail.Situacao nova);
}
