package com.techmentor.api.repository;

import com.techmentor.api.model.Conquista;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.model.UsuarioConquista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioConquistaRepository extends JpaRepository<UsuarioConquista, Long> {
    List<UsuarioConquista> findByUsuario(Usuario usuario);
    boolean existsByUsuarioAndConquista(Usuario usuario, Conquista conquista);
    void deleteByConquista(Conquista conquista);
}