package com.techmentor.api.repository;

import com.techmentor.api.model.Mentoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentoriaRepository extends JpaRepository<Mentoria, Long> {
    List<Mentoria> findByAlunaIdOrderByHorarioInicioDesc(Long alunaId);
    List<Mentoria> findByHorarioMentoraUsuarioIdOrderByHorarioInicioDesc(Long usuarioId);
    boolean existsByHorarioMentoraId(Long mentoraId);
}
