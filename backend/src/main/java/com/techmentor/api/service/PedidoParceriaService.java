package com.techmentor.api.service;

import com.techmentor.api.dto.ParceiraDtos.ConviteRequest;
import com.techmentor.api.dto.ParceiraDtos.ParceiraResponse;
import com.techmentor.api.dto.PedidoParceriaDtos.PedidoRequest;
import com.techmentor.api.dto.PedidoParceriaDtos.PedidoResponse;
import com.techmentor.api.evento.EventosDeNotificacao.PedidoDeParceriaRecebido;
import com.techmentor.api.evento.EventosDeNotificacao.PedidoDeParceriaRecusado;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.AcaoAuditada;
import com.techmentor.api.model.PedidoParceria;
import com.techmentor.api.model.RegistroAuditoria;
import com.techmentor.api.model.SituacaoPedido;
import com.techmentor.api.model.TipoParceira;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.ParceiraRepository;
import com.techmentor.api.repository.PedidoParceriaRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O pedido de parceria feito pelo site. A instituição pede (rota pública); a administradora analisa na fila.
 * Aprovar cria a conta de parceira e dispara o convite que já existe (código e link por e-mail).
 * Recusar guarda o motivo e avisa a instituição.
 */
@Service
@RequiredArgsConstructor
public class PedidoParceriaService {

    private final PedidoParceriaRepository pedidos;
    private final ParceiraRepository parceiras;
    private final UsuarioRepository usuarios;
    private final ParceiraService parceiraService;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    // ------------------------------------------------------------------ a instituição (rota pública)

    /**
     * Guarda o pedido e avisa a equipe. Duas proteções que ninguém percebe:
     * - o campo escondido "website" preenchido é robô: finge que deu certo e não guarda nada;
     * - pedido repetido, e-mail que já tem conta ou nome/CNPJ de quem já é parceira: também responde "recebido" e não
     *   cria nada, para a rota não servir de lista de quem já se cadastrou.
     * Dado inválido (CNPJ, campos) continua recusado com 400, porque a pessoa precisa saber o que corrigir.
     */
    @Transactional
    public void registrar(PedidoRequest dados) {
        if (dados.website() != null && !dados.website().isBlank()) return;

        if (!Boolean.TRUE.equals(dados.aceitaPrivacidade())) {
            throw new BadRequestException("É preciso aceitar a política de privacidade para enviar o pedido.");
        }

        String documento = Cnpj.soDigitos(dados.documento());
        if (documento == null && dados.tipo() == TipoParceira.EMPRESA) {
            throw new BadRequestException("Empresa precisa informar o CNPJ.");
        }
        if (documento != null && !Cnpj.valido(documento)) {
            throw new BadRequestException("CNPJ inválido.");
        }

        String email = Emails.normalizar(dados.email());
        String nome = dados.nome().trim();

        boolean jaExiste = usuarios.existsByEmail(email)
                || parceiras.existsByNomeIgnoreCase(nome)
                || (documento != null && parceiras.existsByDocumento(documento))
                || pedidos.existsByEmailIgnoreCaseAndSituacao(email, SituacaoPedido.PENDENTE)
                || (documento != null && pedidos.existsByDocumentoAndSituacao(documento, SituacaoPedido.PENDENTE));
        if (jaExiste) return;

        PedidoParceria pedido = new PedidoParceria();
        pedido.setNome(nome);
        pedido.setTipo(dados.tipo());
        pedido.setDocumento(documento);
        pedido.setSite(vazioComoNulo(dados.site()));
        pedido.setResponsavel(dados.responsavel().trim());
        pedido.setEmail(email);
        pedido.setMensagem(dados.mensagem().trim());
        pedido.setPoliticaVersao(dados.versaoPolitica().trim());
        pedido = pedidos.save(pedido);

        auditoria.registrar(AcaoAuditada.PEDIDO_RECEBIDO, RegistroAuditoria.PEDIDO, pedido.getId(), "\"" + nome + "\"");
        eventos.publishEvent(new PedidoDeParceriaRecebido(pedido.getId(), nome, pedido.getResponsavel(), email,
                pedido.getTipo().name(), pedido.getSite(), pedido.getMensagem()));
    }

    // ------------------------------------------------------------------ a administradora

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Usuario ator, SituacaoPedido filtro) {
        RegraAcesso.exigirAdmin(ator);
        List<PedidoParceria> lista = filtro == null
                ? pedidos.findAllByOrderByCriadoEmDesc()
                : pedidos.findBySituacaoOrderByCriadoEmDesc(filtro);
        return lista.stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse detalhar(Usuario ator, Long id) {
        RegraAcesso.exigirAdmin(ator);
        return resposta(pedido(id));
    }

    /**
     * Aprova: cria a conta de parceira (inativa, esperando o aceite) e manda o convite por e-mail, exatamente como
     * se a administradora tivesse convidado direto. Se o e-mail, o nome ou o CNPJ já existem, o convite é recusado
     * com 409 e o pedido continua pendente.
     */
    @Transactional
    public PedidoResponse aprovar(Usuario ator, Long id) {
        RegraAcesso.exigirAdmin(ator);
        PedidoParceria pedido = pedido(id);
        exigirPendente(pedido);

        ParceiraResponse criada = parceiraService.convidar(ator, new ConviteRequest(pedido.getNome(), pedido.getTipo(),
                pedido.getDocumento(), pedido.getSite(), pedido.getResponsavel(), pedido.getEmail()));

        pedido.setSituacao(SituacaoPedido.APROVADO);
        pedido.setDecididoEm(LocalDateTime.now());
        pedido.setDecididoPor(ator);
        pedido.setParceira(parceiras.getReferenceById(criada.id()));

        auditoria.registrar(ator, AcaoAuditada.PEDIDO_APROVADO, RegistroAuditoria.PEDIDO, id,
                "Parceira #" + criada.id() + " convidada.");
        return resposta(pedido);
    }

    @Transactional
    public PedidoResponse recusar(Usuario ator, Long id, String motivo) {
        RegraAcesso.exigirAdmin(ator);
        PedidoParceria pedido = pedido(id);
        exigirPendente(pedido);

        pedido.setSituacao(SituacaoPedido.RECUSADO);
        pedido.setDecididoEm(LocalDateTime.now());
        pedido.setDecididoPor(ator);
        pedido.setMotivoDaRecusa(motivo.trim());

        auditoria.registrar(ator, AcaoAuditada.PEDIDO_RECUSADO, RegistroAuditoria.PEDIDO, id, motivo.trim());
        eventos.publishEvent(new PedidoDeParceriaRecusado(pedido.getNome(), pedido.getResponsavel(), pedido.getEmail(), motivo.trim()));
        return resposta(pedido);
    }

    // ------------------------------------------------------------------ apoio

    private PedidoParceria pedido(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new NotFoundException("Pedido não encontrado!"));
    }

    private static void exigirPendente(PedidoParceria pedido) {
        if (pedido.getSituacao() != SituacaoPedido.PENDENTE) {
            throw new ConflictException("Este pedido já foi respondido.");
        }
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private PedidoResponse resposta(PedidoParceria p) {
        return new PedidoResponse(p.getId(), p.getNome(), p.getTipo(), p.getDocumento(), p.getSite(), p.getResponsavel(),
                p.getEmail(), p.getMensagem(), p.getSituacao(), p.getCriadoEm(),
                ParceiraService.dominioConfere(p.getEmail(), p.getSite()),
                p.getConsentimentoEm(), p.getPoliticaVersao(), p.getDecididoEm(),
                p.getDecididoPor() == null ? null : p.getDecididoPor().getNome(),
                p.getMotivoDaRecusa(), p.getParceira() == null ? null : p.getParceira().getId());
    }
}
