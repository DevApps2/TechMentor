package com.techmentor.api.repository;

import com.techmentor.api.model.Desafio;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.model.UsuarioDesafio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioDesafioRepository extends JpaRepository<UsuarioDesafio, Long> {
    List<UsuarioDesafio> findByUsuario(Usuario usuario);
    boolean existsByUsuarioAndDesafio(Usuario usuario, Desafio desafio);
    Optional<UsuarioDesafio> findByUsuarioAndDesafio(Usuario usuario, Desafio desafio);
    void deleteByDesafio(Desafio desafio);

    List<UsuarioDesafio> findByUsuarioAndStatus(Usuario usuario, UsuarioDesafio.Status status);
}