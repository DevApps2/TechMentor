package com.techmentor.api.repository;

import com.techmentor.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    Optional<Usuario> findByIdAndAtivoTrue(Long id);

    List<Usuario> findByAtivoTrue();

    Optional<Usuario> findByEmail(String email);
}