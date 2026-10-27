package com.techmentor.api.repository;

import com.techmentor.api.model.Oportunidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long> {
    List<Oportunidade> findByAtivaTrueOrderByPrazoInscricaoAsc();
}
