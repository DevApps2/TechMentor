package com.techmentor.api.service;

import com.techmentor.api.dto.SegurancaDtos.ConteudoSegurancaResponse;
import com.techmentor.api.model.ConteudoSeguranca;
import com.techmentor.api.repository.ConteudoSegurancaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConteudoSegurancaService {

    private final ConteudoSegurancaRepository repository;

    /** Orientações e canais de ajuda da tela de Segurança; sem o tipo, devolve tudo. */
    @Transactional(readOnly = true)
    public List<ConteudoSegurancaResponse> listar(ConteudoSeguranca.Tipo tipo) {
        List<ConteudoSeguranca> itens = tipo == null
                ? repository.findAllByOrderByTipoAscIdAsc()
                : repository.findByTipoOrderByIdAsc(tipo);
        return itens.stream().map(ConteudoSegurancaResponse::from).toList();
    }
}
