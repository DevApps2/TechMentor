package com.techmentor.api.repository;

import com.techmentor.api.model.Mentoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Mentorias nesta situação cujo horário já terminou (usado para marcar como concluídas). */
    List<Mentoria> findByStatusAndHorarioFimBefore(Mentoria.Status status, java.time.LocalDateTime limite);

    /** Mentorias nesta situação cujo horário já começou (usado para expirar as que ninguém confirmou). */
    List<Mentoria> findByStatusAndHorarioInicioBefore(Mentoria.Status status, java.time.LocalDateTime limite);

    /**
     * Mentorias do usuário, no status pedido, que terminaram depois do limite e que ele ainda não avaliou.
     * É a lista de "avaliações pendentes".
     */
    @Query("""
            select m from Mentoria m
            where m.status = :status
              and m.horario.fim > :limite
              and (m.aluna.id = :usuarioId or m.horario.mentora.usuario.id = :usuarioId)
              and not exists (select a.id from Avaliacao a where a.mentoria = m and a.avaliador.id = :usuarioId)
            order by m.horario.fim desc
            """)
    List<Mentoria> pendentesDeAvaliacao(@Param("usuarioId") Long usuarioId,
                                        @Param("status") Mentoria.Status status,
                                        @Param("limite") java.time.LocalDateTime limite);

    /** Concluídas que ainda não receberam o lembrete de avaliação e terminaram dentro do período. */
    List<Mentoria> findByStatusAndLembreteAvaliacaoEnviadoEmIsNullAndHorarioFimBetween(
            Mentoria.Status status, java.time.LocalDateTime de, java.time.LocalDateTime ate);
}