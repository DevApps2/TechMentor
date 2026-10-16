package com.techmentor.api.repository;

import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Matricula;
import com.techmentor.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    boolean existsByUsuarioAndCurso(Usuario usuario, Curso curso);

    List<Matricula> findByUsuario(Usuario usuario);

    Optional<Matricula> findByUsuarioAndCurso(Usuario usuario, Curso curso);

}
