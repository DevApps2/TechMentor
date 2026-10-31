package com.techmentor.api.service;

import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.dto.OportunidadeDtos.*;
import com.techmentor.api.repository.InscricaoOportunidadeRepository;
import com.techmentor.api.repository.OportunidadeRepository;
import com.techmentor.api.exceptions.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class OportunidadeService {
    private final OportunidadeRepository oportunidades;
    private final InscricaoOportunidadeRepository inscricoes;
    private final ConquistaService conquistas;

    public OportunidadeService(OportunidadeRepository oportunidades,
                               InscricaoOportunidadeRepository inscricoes,
                               ConquistaService conquistas) {
        this.oportunidades = oportunidades;
        this.inscricoes = inscricoes;
        this.conquistas = conquistas;
    }

    /** Oportunidades ativas pelo prazo mais próximo; filtros opcionais por tipo e por área. */
    @Transactional(readOnly = true)
    public List<OportunidadeResponse> listar(Oportunidade.Tipo tipo, String area) {
        return oportunidades.findByAtivaTrueOrderByPrazoInscricaoAsc().stream()
                .filter(o -> tipo == null || o.getTipo() == tipo)
                .filter(o -> area == null || area.isBlank() || area.trim().equalsIgnoreCase(o.getArea()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OportunidadeResponse obter(Long id) {
        Oportunidade oportunidade = buscar(id);
        if (!oportunidade.isAtiva()) throw ApiException.notFound("Oportunidade não encontrada.");
        return toResponse(oportunidade);
    }

    public OportunidadeResponse criar(Usuario actor, OportunidadeRequest request) {
        RegraAcesso.exigirAdmin(actor);
        validar(request);
        return toResponse(oportunidades.save(aplicar(new Oportunidade(), request)));
    }

    public OportunidadeResponse atualizar(Usuario actor, Long id, OportunidadeRequest request) {
        RegraAcesso.exigirAdmin(actor);
        validar(request);
        Oportunidade oportunidade = buscar(id);
        if (!oportunidade.isAtiva()) throw ApiException.notFound("Oportunidade não encontrada.");
        return toResponse(aplicar(oportunidade, request));
    }

    public void excluir(Usuario actor, Long id) {
        RegraAcesso.exigirAdmin(actor);
        buscar(id).setAtiva(false);
    }

    public InscricaoResponse candidatar(Usuario actor, Long oportunidadeId) {
        return inscrever(actor, oportunidadeId, false);
    }

    public InscricaoResponse inscreverEmEvento(Usuario actor, Long oportunidadeId) {
        return inscrever(actor, oportunidadeId, true);
    }

    private InscricaoResponse inscrever(Usuario actor, Long id, boolean evento) {
        Oportunidade oportunidade = buscar(id);
        if (!oportunidade.isAtiva() || (oportunidade.getPrazoInscricao() != null
                && oportunidade.getPrazoInscricao().isBefore(LocalDate.now())))
            throw ApiException.conflict("Oportunidade encerrada.");
        if ((oportunidade.getTipo() == Oportunidade.Tipo.EVENTO) != evento)
            throw ApiException.badRequest(evento
                    ? "Inscrições são permitidas apenas para eventos."
                    : "Candidaturas são permitidas apenas para vagas e bolsas.");
        if (inscricoes.findByUsuarioIdAndOportunidadeId(actor.getId(), id).isPresent())
            throw ApiException.conflict("Usuário já possui inscrição nesta oportunidade.");
        InscricaoOportunidade inscricao = new InscricaoOportunidade();
        inscricao.setUsuario(actor);
        inscricao.setOportunidade(oportunidade);
        InscricaoOportunidade salva = inscricoes.save(inscricao);
        conquistas.desbloquear(actor, ConquistaService.PRIMEIRA_CANDIDATURA);
        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> minhasInscricoes(Usuario actor, boolean eventos) {
        return inscricoes.findByUsuarioIdOrderByCriadaEmDesc(actor.getId()).stream()
                .filter(i -> (i.getOportunidade().getTipo() == Oportunidade.Tipo.EVENTO) == eventos)
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InscricaoResponse obterCandidatura(Usuario actor, Long id) {
        InscricaoOportunidade inscricao = inscricoes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Candidatura não encontrada."));
        RegraAcesso.exigirProprietarioOuAdmin(actor, inscricao.getUsuario().getId());
        return toResponse(inscricao);
    }

    private Oportunidade buscar(Long id) {
        return oportunidades.findById(id).orElseThrow(() -> ApiException.notFound("Oportunidade não encontrada."));
    }

    private void validar(OportunidadeRequest request) {
        if (request == null) throw ApiException.badRequest("Dados da oportunidade são obrigatórios.");
        if (request.tipo() == null || request.titulo() == null || request.titulo().isBlank())
            throw ApiException.badRequest("Tipo e título são obrigatórios.");
        if (request.prazoInscricao() != null && request.prazoInscricao().isBefore(LocalDate.now()))
            throw ApiException.badRequest("Prazo não pode estar no passado.");
    }

    private Oportunidade aplicar(Oportunidade oportunidade, OportunidadeRequest request) {
        oportunidade.setTipo(request.tipo());
        oportunidade.setTitulo(request.titulo().trim());
        oportunidade.setOrganizacao(request.organizacao());
        oportunidade.setDescricao(request.descricao());
        oportunidade.setRequisitos(request.requisitos());
        oportunidade.setBeneficios(request.beneficios());
        oportunidade.setArea(request.area());
        oportunidade.setNivel(request.nivel());
        oportunidade.setLocalidade(request.localidade());
        oportunidade.setLinkExterno(request.linkExterno());
        oportunidade.setPrazoInscricao(request.prazoInscricao());
        oportunidade.setAtiva(true);
        return oportunidade;
    }

    private OportunidadeResponse toResponse(Oportunidade oportunidade) {
        return new OportunidadeResponse(oportunidade.getId(), oportunidade.getTipo(), oportunidade.getTitulo(),
                oportunidade.getOrganizacao(), oportunidade.getDescricao(), oportunidade.getRequisitos(),
                oportunidade.getLocalidade(), oportunidade.getLinkExterno(), oportunidade.getPrazoInscricao(),
                oportunidade.isAtiva(), oportunidade.getBeneficios(), oportunidade.getArea(), oportunidade.getNivel());
    }

    private InscricaoResponse toResponse(InscricaoOportunidade inscricao) {
        return new InscricaoResponse(inscricao.getId(), inscricao.getUsuario().getId(), inscricao.getOportunidade().getId(),
                inscricao.getOportunidade().getTitulo(), inscricao.getStatus(), inscricao.getCriadaEm());
    }
}
