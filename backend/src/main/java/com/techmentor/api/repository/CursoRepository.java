package com.techmentor.api.repository;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Parceira;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findAllByAtivoTrue();

    List<Curso> findByAtivoTrueAndCategoria(Curso.Categoria categoria);
    List<Curso> findAllByAtivoFalse();
    List<Curso> findByParceira(Parceira parceira);
    boolean existsByTituloIgnoreCase(String titulo);
    Optional<Curso> findByTituloIgnoreCase(String titulo);
}