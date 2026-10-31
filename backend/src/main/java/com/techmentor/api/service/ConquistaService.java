package com.techmentor.api.service;

import com.techmentor.api.dto.GamificacaoDtos.ConquistaRequest;
import com.techmentor.api.dto.GamificacaoDtos.ConquistaResponse;
import com.techmentor.api.dto.GamificacaoDtos.UsuarioConquistaResponse;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Conquista;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.model.UsuarioConquista;
import com.techmentor.api.repository.ConquistaRepository;
import com.techmentor.api.repository.UsuarioConquistaRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConquistaService {

    // Conquistas desbloqueadas automaticamente pelo sistema (os nomes precisam existir no cadastro de conquistas).
    public static final String PRIMEIRA_AULA = "Primeira aula";
    public static final String CURSO_CONCLUIDO = "Curso concluído";
    public static final String SEQUENCIA_3_DIAS = "Sequência de 3 dias";
    public static final String PRIMEIRA_MENTORIA = "Primeira mentoria";
    public static final String PRIMEIRO_DESAFIO = "Primeiro desafio";
    public static final String PRIMEIRA_CANDIDATURA = "Explorando oportunidades";

    private final ConquistaRepository conquistaRepository;
    private final UsuarioConquistaRepository usuarioConquistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;
    private final XpService xpService;

    public Conquista criarConquista(Conquista conquista) {
        if (conquistaRepository.existsByNomeIgnoreCase(conquista.getNome())) {
            throw new ConflictException("Já existe uma conquista com esse nome!");
        }
        conquista.setId(null);
        return conquistaRepository.save(conquista);
    }

    @Transactional
    public ConquistaResponse criarConquista(ConquistaRequest request) {
        return ConquistaResponse.from(criarConquista(request.toEntity()));
    }

    public List<Conquista> listarConquistas() {
        return conquistaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ConquistaResponse> listarConquistasDTO() {
        return listarConquistas().stream().map(ConquistaResponse::from).toList();
    }

    public List<UsuarioConquista> listarConquistasDaUsuaria(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        return usuarioConquistaRepository.findByUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioConquistaResponse> listarConquistasDaUsuariaDTO(Long usuarioId) {
        return listarConquistasDaUsuaria(usuarioId).stream().map(UsuarioConquistaResponse::from).toList();
    }

    public Conquista atualizarConquista(String nome, Conquista dados) {
        Conquista conquista = conquistaRepository.findByNomeIgnoreCase(nome)
                .orElseThrow(() -> new NotFoundException("Conquista não encontrada!"));

        if (!conquista.getNome().equalsIgnoreCase(dados.getNome())
                && conquistaRepository.existsByNomeIgnoreCase(dados.getNome())) {
            throw new ConflictException("Já existe uma conquista com esse nome!");
        }

        conquista.setNome(dados.getNome());
        conquista.setDescricao(dados.getDescricao());
        conquista.setIconeUrl(dados.getIconeUrl());
        conquista.setXpBonus(dados.getXpBonus());
        return conquistaRepository.save(conquista);
    }

    @Transactional
    public ConquistaResponse atualizarConquista(String nome, ConquistaRequest request) {
        return ConquistaResponse.from(atualizarConquista(nome, request.toEntity()));
    }

    @Transactional
    public void deletarConquista(String nome) {
        Conquista conquista = conquistaRepository.findByNomeIgnoreCase(nome)
                .orElseThrow(() -> new NotFoundException("Conquista não encontrada!"));
        usuarioConquistaRepository.deleteByConquista(conquista);
        conquistaRepository.delete(conquista);
    }

    public void desbloquear(Usuario usuario, String nomeConquista) {
        Conquista conquista = conquistaRepository.findByNomeIgnoreCase(nomeConquista).orElse(null);
        if (conquista == null || usuarioConquistaRepository.existsByUsuarioAndConquista(usuario, conquista)) {
            return;
        }
        UsuarioConquista desbloqueio = new UsuarioConquista();
        desbloqueio.setUsuario(usuario);
        desbloqueio.setConquista(conquista);
        usuarioConquistaRepository.save(desbloqueio);

        xpService.registrar(usuario, conquista.getXpBonus(), "Conquista: " + conquista.getNome());
        usuarioRepository.save(usuario);
        notificacaoService.notificar(usuario, "Conquista desbloqueada!", conquista.getNome());
    }
}
