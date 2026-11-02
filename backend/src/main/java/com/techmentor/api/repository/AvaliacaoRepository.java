package com.techmentor.api.repository;

import com.techmentor.api.model.Avaliacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    boolean existsByMentoriaIdAndAvaliadorId(Long mentoriaId, Long avaliadorId);

    List<Avaliacao> findByMentoriaId(Long mentoriaId);

    /** O que escreveram sobre a pessoa (sem as ocultadas pela moderação), da mais nova para a mais antiga. */
    List<Avaliacao> findByAvaliadoIdAndOcultaFalseOrderByCriadaEmDesc(Long avaliadoId);

    /** Avaliações públicas de uma mentora (as das alunas, sem as ocultadas), limitadas pelo Pageable. */
    List<Avaliacao> findByAvaliadoIdAndAutorAndOcultaFalseOrderByCriadaEmDesc(
            Long avaliadoId, Avaliacao.Autor autor, Pageable pageable);

    /** Quantas avaliações de cada nota o avaliado recebeu da aluna (conta também as ocultadas: a nota vale na média). */
    @Query("select a.nota, count(a) from Avaliacao a where a.avaliado.id = :avaliadoId and a.autor = :autor group by a.nota")
    List<Object[]> contarPorNota(@Param("avaliadoId") Long avaliadoId, @Param("autor") Avaliacao.Autor autor);
}