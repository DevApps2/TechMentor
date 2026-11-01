package com.techmentor.api.repository;

import com.techmentor.api.model.Oportunidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long> {

    /**
     * Oportunidades ativas e ainda abertas: sem prazo, ou com prazo de hoje em diante.
     * Prazo mais próximo primeiro; as sem prazo vão para o fim da lista.
     */
    @Query("select o from Oportunidade o "
            + "where o.ativa = true and (o.prazoInscricao is null or o.prazoInscricao >= :hoje) "
            + "order by case when o.prazoInscricao is null then 1 else 0 end, o.prazoInscricao asc")
    List<Oportunidade> listarAbertas(@Param("hoje") LocalDate hoje);
}
