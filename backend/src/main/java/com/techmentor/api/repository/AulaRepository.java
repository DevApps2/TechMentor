package com.techmentor.api.repository;

import com.techmentor.api.model.Aula;
import com.techmentor.api.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    boolean existsByModuloAndOrdem(Modulo modulo, int ordem);

    Optional<Aula> findByModuloAndOrdem(Modulo modulo, int ordem);

}
