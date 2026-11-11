package com.techmentor.api.service;

import com.techmentor.api.evento.EventosDeNotificacao;
import com.techmentor.api.model.Avaliacao;
import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.dto.SegurancaDtos.DenunciaRequest;
import com.techmentor.api.dto.SegurancaDtos.DenunciaResponse;
import com.techmentor.api.repository.AvaliacaoRepository;
import com.techmentor.api.repository.DenunciaRepository;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.exceptions.TooManyRequestsException;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DenunciaService {

    // Limites contra abuso: pouca denúncia por hora e só uma por pessoa a cada 24 horas
    static final int MAXIMO_POR_HORA = 5;
    static final int HORAS_ENTRE_DENUNCIAS_DA_MESMA_PESSOA = 24;

    private final DenunciaRepository denuncias;
    private final UsuarioRepository usuarios;
    private final AvaliacaoRepository avaliacoes;
    private final ApplicationEventPublisher eventos;

    public DenunciaService(DenunciaRepository denuncias, UsuarioRepository usuarios, AvaliacaoRepository avaliacoes,
                           NotificacaoService notificacoes, ApplicationEventPublisher eventos) {
        this.denuncias = denuncias;
        this.usuarios = usuarios;
        this.avaliacoes = avaliacoes;
        this.eventos = eventos;
    }

    public DenunciaResponse criar(Usuario actor, DenunciaRequest request) {
        if (request == null || request.motivo() == null)
            throw new BadRequestException("Motivo da denúncia é obrigatório.");

        Avaliacao avaliacao = null;
        Usuario alvo;
        if (request.avaliacaoId() != null) {
            avaliacao = avaliacoes.findById(request.avaliacaoId())
                    .orElseThrow(() -> new NotFoundException("Avaliação não encontrada."));
            // Só quem foi avaliada denuncia o comentário: quem escreveu ou terceiros não
            if (!avaliacao.getAvaliado().getId().equals(actor.getId())) throw new ForbiddenException();
            if (avaliacao.getComentario() == null || avaliacao.getComentario().isBlank())
                throw new BadRequestException("Esta avaliação não tem comentário para denunciar.");
            if (denuncias.existsByDenuncianteIdAndAvaliacaoId(actor.getId(), avaliacao.getId()))
                throw new ConflictException("Você já denunciou este comentário.");
            alvo = avaliacao.getAvaliador(); // o denunciado é quem escreveu, não o que o app mandar
        } else {
            alvo = request.usuarioDenunciadoId() == null ? null : buscarUsuario(request.usuarioDenunciadoId());
        }
        if (alvo != null && alvo.getId().equals(actor.getId()))
            throw new BadRequestException("Não é possível denunciar a si mesmo.");

        LocalDateTime agora = LocalDateTime.now();
        if (denuncias.countByDenuncianteIdAndCriadaEmAfter(actor.getId(), agora.minusHours(1)) >= MAXIMO_POR_HORA)
            throw new TooManyRequestsException("Você enviou muitas denúncias em pouco tempo. Tente de novo mais tarde.");
        if (alvo != null && avaliacao == null && denuncias.existsByDenuncianteIdAndDenunciadoIdAndCriadaEmAfter(
                actor.getId(), alvo.getId(), agora.minusHours(HORAS_ENTRE_DENUNCIAS_DA_MESMA_PESSOA)))
            throw new ConflictException("Você já denunciou esta pessoa nas últimas 24 horas.");

        Denuncia denuncia = new Denuncia();
        denuncia.setDenunciante(actor);
        denuncia.setDenunciado(alvo);
        denuncia.setAvaliacao(avaliacao);
        denuncia.setMotivo(request.motivo());
        denuncia.setDescricao(request.descricao());
        Denuncia salva = denuncias.save(denuncia);

        // Avisa a equipe por e-mail (só depois que a denúncia for gravada de verdade)
        eventos.publishEvent(new DenunciaCriadaEvent(
                salva.getId(), salva.getMotivo().name(), salva.getDescricao(), salva.getCriadaEm(),
                actor.getId(), actor.getNome(),
                alvo == null ? null : alvo.getId(), alvo == null ? null : alvo.getNome()));

        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<DenunciaResponse> minhas(Usuario actor) {
        return denuncias.findByDenuncianteIdOrderByCriadaEmDesc(actor.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DenunciaResponse> listarTodas(Usuario actor) {
        RegraAcesso.exigirAdmin(actor);
        return denuncias.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DenunciaResponse obter(Usuario actor, Long id) {
        Denuncia denuncia = denuncias.findById(id).orElseThrow(() -> new NotFoundException("Denúncia não encontrada."));
        if (actor.getTipo() != Usuario.Tipo.ADMIN && !denuncia.getDenunciante().getId().equals(actor.getId()))
            throw new ForbiddenException();
        return toResponse(denuncia);
    }

    /** Só administradores. Muda a situação e, se ela mudou de fato, avisa quem denunciou por notificação. */
    public DenunciaResponse atualizarStatus(Usuario actor, Long id, Denuncia.Status novo) {
        RegraAcesso.exigirAdmin(actor);
        if (novo == null) throw new BadRequestException("Informe a nova situação da denúncia.");

        Denuncia denuncia = denuncias.findById(id).orElseThrow(() -> new NotFoundException("Denúncia não encontrada."));

        Denuncia.Status anterior = denuncia.getStatus();
        denuncia.setStatus(novo);

        if (anterior != novo) {
            String nomeDoDenunciado = denuncia.getDenunciado() == null ? null : denuncia.getDenunciado().getNome();
            eventos.publishEvent(new EventosDeNotificacao.StatusDaDenunciaMudou(denuncia.getDenunciante(), nomeDoDenunciado, novo));
        }

        return toResponse(denuncia);
    }

    private static String mensagemDoStatus(Denuncia denuncia, Denuncia.Status status) {
        String sobre = denuncia.getDenunciado() == null ? "" : " sobre " + denuncia.getDenunciado().getNome();
        return switch (status) {
            case EM_ANALISE -> "Sua denúncia" + sobre + " está em análise pela equipe.";
            case RESOLVIDA -> "Sua denúncia" + sobre + " foi resolvida. Obrigada por ajudar a manter o TechMentor seguro.";
            case ABERTA -> "Sua denúncia" + sobre + " foi reaberta.";
        };
    }

    private Usuario buscarUsuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private DenunciaResponse toResponse(Denuncia denuncia) {
        Usuario denunciado = denuncia.getDenunciado();
        Avaliacao avaliacao = denuncia.getAvaliacao();
        return new DenunciaResponse(denuncia.getId(), denuncia.getDenunciante().getId(),
                denunciado == null ? null : denunciado.getId(),
                denunciado == null ? null : denunciado.getNome(),
                denuncia.getMotivo(), denuncia.getDescricao(), denuncia.getStatus(), denuncia.getCriadaEm(),
                avaliacao == null ? null : avaliacao.getId(),
                avaliacao == null ? null : avaliacao.getNota(),
                avaliacao == null ? null : avaliacao.getComentario(),
                avaliacao == null ? null : avaliacao.isOculta());
    }
}