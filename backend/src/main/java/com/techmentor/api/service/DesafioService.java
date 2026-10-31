package com.techmentor.api.service;

import com.techmentor.api.dto.GamificacaoDtos.DesafioRequest;
import com.techmentor.api.dto.GamificacaoDtos.DesafioResponse;
import com.techmentor.api.dto.GamificacaoDtos.UsuarioDesafioResponse;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Desafio;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.model.UsuarioDesafio;
import com.techmentor.api.repository.DesafioRepository;
import com.techmentor.api.repository.MentoriaRepository;
import com.techmentor.api.repository.ProgressoAulaRepository;
import com.techmentor.api.repository.UsuarioDesafioRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesafioService {

    private final DesafioRepository desafioRepository;
    private final UsuarioDesafioRepository usuarioDesafioRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;
    private final XpService xpService;
    private final ProgressoAulaRepository progressoAulaRepository;
    private final ConquistaService conquistaService;
    private final MentoriaRepository mentoriaRepository;


    public Desafio criarDesafio(Desafio desafio) {
        if (desafioRepository.existsByTituloIgnoreCase(desafio.getTitulo())) {
            throw new ConflictException("Já existe um desafio com esse título!");
        }
        desafio.setId(null);
        return desafioRepository.save(desafio);
    }

    @Transactional
    public DesafioResponse criarDesafio(DesafioRequest request) {
        return DesafioResponse.from(criarDesafio(request.toEntity()));
    }

    public List<Desafio> listarDesafiosAtivos() {
        LocalDate hoje = LocalDate.now();
        return desafioRepository.findByInicioLessThanEqualAndFimGreaterThanEqual(hoje, hoje);
    }

    @Transactional(readOnly = true)
    public List<DesafioResponse> listarDesafiosAtivosDTO() {
        return listarDesafiosAtivos().stream().map(DesafioResponse::from).toList();
    }

    public UsuarioDesafio participar(String titulo, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        Desafio desafio = desafioRepository.findByTituloIgnoreCase(titulo)
                .orElseThrow(() -> new NotFoundException("Desafio não encontrado!"));

        LocalDate hoje = LocalDate.now();
        if (desafio.getInicio().isAfter(hoje) || desafio.getFim().isBefore(hoje)) {
            throw new NotFoundException("Desafio não está ativo!");
        }

        if (usuarioDesafioRepository.existsByUsuarioAndDesafio(usuario, desafio)) {
            throw new ConflictException("Usuária já participa desse desafio!");
        }

        UsuarioDesafio participacao = new UsuarioDesafio();
        participacao.setUsuario(usuario);
        participacao.setDesafio(desafio);
        return usuarioDesafioRepository.save(participacao);
    }

    @Transactional
    public UsuarioDesafioResponse participarDTO(String titulo, Long usuarioId) {
        UsuarioDesafio participacao = participar(titulo, usuarioId);
        participacao.setAulasConcluidas(contarProgresso(participacao.getUsuario(), participacao.getDesafio()));
        return UsuarioDesafioResponse.from(participacao);
    }

    public List<UsuarioDesafio> listarDesafiosDaUsuaria(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        List<UsuarioDesafio> lista = usuarioDesafioRepository.findByUsuario(usuario);
        lista.forEach(ud -> ud.setAulasConcluidas(contarProgresso(usuario, ud.getDesafio())));
        return lista;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDesafioResponse> listarDesafiosDaUsuariaDTO(Long usuarioId) {
        return listarDesafiosDaUsuaria(usuarioId).stream().map(UsuarioDesafioResponse::from).toList();
    }

    public Desafio atualizarDesafio(String titulo, Desafio dados) {
        Desafio desafio = desafioRepository.findByTituloIgnoreCase(titulo)
                .orElseThrow(() -> new NotFoundException("Desafio não encontrado!"));

        if (!desafio.getTitulo().equalsIgnoreCase(dados.getTitulo())
                && desafioRepository.existsByTituloIgnoreCase(dados.getTitulo())) {
            throw new ConflictException("Já existe um desafio com esse título!");
        }

        desafio.setTitulo(dados.getTitulo());
        desafio.setDescricao(dados.getDescricao());
        desafio.setXpRecompensa(dados.getXpRecompensa());
        desafio.setInicio(dados.getInicio());
        desafio.setFim(dados.getFim());
        desafio.setMetaAulas(dados.getMetaAulas());
        desafio.setTipo(dados.getTipo());
        return desafioRepository.save(desafio);
    }

    @Transactional
    public DesafioResponse atualizarDesafio(String titulo, DesafioRequest request) {
        return DesafioResponse.from(atualizarDesafio(titulo, request.toEntity()));
    }

    @Transactional
    public void deletarDesafio(String titulo) {
        Desafio desafio = desafioRepository.findByTituloIgnoreCase(titulo)
                .orElseThrow(() -> new NotFoundException("Desafio não encontrado!"));
        usuarioDesafioRepository.deleteByDesafio(desafio);
        desafioRepository.delete(desafio);
    }

    // Uso interno (ainda sem regra que chame): marca como concluído e paga o XP Não é endpoint.
    @Transactional
    public void verificarDesafios(Usuario usuario) {
        for (UsuarioDesafio participacao :
                usuarioDesafioRepository.findByUsuarioAndStatus(usuario, UsuarioDesafio.Status.EM_ANDAMENTO)) {
            Desafio desafio = participacao.getDesafio();
            if (contarProgresso(usuario, desafio) >= desafio.getMetaAulas()) {
                participacao.setStatus(UsuarioDesafio.Status.CONCLUIDO);
                participacao.setConcluidoEm(LocalDateTime.now());
                usuarioDesafioRepository.save(participacao);

                xpService.registrar(usuario, desafio.getXpRecompensa(), "Desafio: " + desafio.getTitulo());
                notificacaoService.notificar(usuario, "Desafio concluído!", desafio.getTitulo());
                conquistaService.desbloquear(usuario, ConquistaService.PRIMEIRO_DESAFIO);
            }
        }
    }

    /** Aulas concluídas ou mentorias agendadas (conforme o tipo do desafio) dentro do período dele. */
    private long contarProgresso(Usuario usuario, Desafio desafio) {
        LocalDateTime de = desafio.getInicio().atStartOfDay();
        LocalDateTime ate = desafio.getFim().plusDays(1).atStartOfDay();
        if (desafio.getTipo() == Desafio.Tipo.MENTORIA) {
            return mentoriaRepository.countByAlunaIdAndStatusNotAndCriadaEmBetween(
                    usuario.getId(), Mentoria.Status.CANCELADA, de, ate);
        }
        return progressoAulaRepository.countByUsuarioAndConcluidaEmBetween(usuario, de, ate);
    }
}
