package com.techmentor.api.repository;

import com.techmentor.api.model.Mentoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentoriaRepository extends JpaRepository<Mentoria, Long> {
    List<Mentoria> findByAlunaIdOrderByHorarioInicioDesc(Long alunaId);
    List<Mentoria> findByHorarioMentoraUsuarioIdOrderByHorarioInicioDesc(Long usuarioId);
    boolean existsByHorarioMentoraId(Long mentoraId);

    /** Mentorias agendadas pela aluna no período, sem contar as canceladas (usado nos desafios de mentoria). */
    long countByAlunaIdAndStatusNotAndCriadaEmBetween(Long alunaId, Mentoria.Status status,
                                                      java.time.LocalDateTime de, java.time.LocalDateTime ate);

    /** Confirmadas que ainda não receberam o lembrete e começam dentro do período. */
    List<Mentoria> findByStatusAndLembreteEnviadoEmIsNullAndHorarioInicioBetween(
            Mentoria.Status status, java.time.LocalDateTime de, java.time.LocalDateTime ate);

    /** Confirmadas sem link de sala que começam dentro do período (a sala é criada de novo). */
    List<Mentoria> findByStatusAndLinkSalaIsNullAndHorarioInicioBetween(
            Mentoria.Status status, java.time.LocalDateTime de, java.time.LocalDateTime ate);
}