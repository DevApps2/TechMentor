package com.techmentor.api.service;

import com.techmentor.api.evento.EventosDeNotificacao;
import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.dto.SegurancaDtos.DenunciaRequest;
import com.techmentor.api.dto.SegurancaDtos.DenunciaResponse;
import com.techmentor.api.repository.DenunciaRepository;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
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
    private final ApplicationEventPublisher eventos;

    public DenunciaService(DenunciaRepository denuncias, UsuarioRepository usuarios,
                           NotificacaoService notificacoes, ApplicationEventPublisher eventos) {
        this.denuncias = denuncias;
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    public DenunciaResponse criar(Usuario actor, DenunciaRequest request) {
        if (request == null || request.motivo() == null)
            throw ApiException.badRequest("Motivo da denúncia é obrigatório.");
        Usuario alvo = request.usuarioDenunciadoId() == null ? null : buscarUsuario(request.usuarioDenunciadoId());
        if (alvo != null && alvo.getId().equals(actor.getId()))
            throw ApiException.badRequest("Não é possível denunciar a si mesmo.");

        LocalDateTime agora = LocalDateTime.now();
        if (denuncias.countByDenuncianteIdAndCriadaEmAfter(actor.getId(), agora.minusHours(1)) >= MAXIMO_POR_HORA)
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Você enviou muitas denúncias em pouco tempo. Tente de novo mais tarde.");
        if (alvo != null && denuncias.existsByDenuncianteIdAndDenunciadoIdAndCriadaEmAfter(
                actor.getId(), alvo.getId(), agora.minusHours(HORAS_ENTRE_DENUNCIAS_DA_MESMA_PESSOA)))
            throw ApiException.conflict("Você já denunciou esta pessoa nas últimas 24 horas.");

        Denuncia denuncia = new Denuncia();
        denuncia.setDenunciante(actor);
        denuncia.setDenunciado(alvo);
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
        Denuncia denuncia = denuncias.findById(id).orElseThrow(() -> ApiException.notFound("Denúncia não encontrada."));
        if (actor.getTipo() != Usuario.Tipo.ADMIN && !denuncia.getDenunciante().getId().equals(actor.getId()))
            throw ApiException.forbidden();
        return toResponse(denuncia);
    }

    /** Só administradores. Muda a situação e, se ela mudou de fato, avisa quem denunciou por notificação. */
    public DenunciaResponse atualizarStatus(Usuario actor, Long id, Denuncia.Status novo) {
        RegraAcesso.exigirAdmin(actor);
        if (novo == null) throw ApiException.badRequest("Informe a nova situação da denúncia.");

        Denuncia denuncia = denuncias.findById(id).orElseThrow(() -> ApiException.notFound("Denúncia não encontrada."));

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
        return usuarios.findById(id).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    }

    private DenunciaResponse toResponse(Denuncia denuncia) {
        Usuario denunciado = denuncia.getDenunciado();
        return new DenunciaResponse(denuncia.getId(), denuncia.getDenunciante().getId(),
                denunciado == null ? null : denunciado.getId(),
                denunciado == null ? null : denunciado.getNome(),
                denuncia.getMotivo(), denuncia.getDescricao(), denuncia.getStatus(), denuncia.getCriadaEm());
    }
}
