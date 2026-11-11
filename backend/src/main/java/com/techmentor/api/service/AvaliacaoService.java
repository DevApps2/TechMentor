package com.techmentor.api.service;

import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoDaMentoriaResponse;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoPendenteResponse;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoPublicaResponse;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoRequest;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoResponse;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacoesDaMentoraResponse;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Avaliacao;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.PerfilMentora;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.AvaliacaoRepository;
import com.techmentor.api.repository.MentoriaRepository;
import com.techmentor.api.repository.PerfilMentoraRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.techmentor.api.evento.EventosDeNotificacao.AvaliacaoRecebida;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Avaliação de mentorias concluídas. Cada pessoa avalia a outra uma única vez, dentro do prazo.
 * A nota da aluna entra na média da mentora; a da mentora sobre a aluna fica guardada e não mexe em média.
 */
@Service
@Transactional
public class AvaliacaoService {

    private static final int LIMITE_MAXIMO_DA_LISTA = 50;

    private final AvaliacaoRepository avaliacoes;
    private final MentoriaRepository mentorias;
    private final PerfilMentoraRepository perfis;
    private final ApplicationEventPublisher eventos;

    public AvaliacaoService(AvaliacaoRepository avaliacoes, MentoriaRepository mentorias,
                            PerfilMentoraRepository perfis, ApplicationEventPublisher eventos) {
        this.avaliacoes = avaliacoes;
        this.mentorias = mentorias;
        this.perfis = perfis;
        this.eventos = eventos;
    }

    public AvaliacaoResponse avaliar(Usuario actor, Long mentoriaId, AvaliacaoRequest request) {
        Mentoria mentoria = buscar(mentoriaId);
        Usuario aluna = mentoria.getAluna();
        Usuario mentora = mentoria.getHorario().getMentora().getUsuario();

        Avaliacao.Autor autor = autorDe(actor, aluna, mentora);

        String impedimento = motivoDeNaoPoder(mentoria, actor.getId());
        if (impedimento != null) throw new ConflictException(impedimento);

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setMentoria(mentoria);
        avaliacao.setAvaliador(actor);
        avaliacao.setAvaliado(autor == Avaliacao.Autor.ALUNA ? mentora : aluna);
        avaliacao.setAutor(autor);
        avaliacao.setNota(request.nota());
        avaliacao.setComentario(limpar(request.comentario()));

        try {
            // flush já: se duas chamadas chegarem juntas, a restrição única recusa a segunda aqui
            avaliacao = avaliacoes.saveAndFlush(avaliacao);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Você já avaliou esta mentoria.");
        }

        if (autor == Avaliacao.Autor.ALUNA) {
            registrarNaMedia(mentora.getId(), request.nota());
        }

        // Avisa quem foi avaliada. A mentora vê a aluna pelo nome abreviado, como no perfil público;
        // a aluna vê a mentora pelo nome inteiro.
        Usuario avaliado = avaliacao.getAvaliado();
        String quem = autor == Avaliacao.Autor.ALUNA ? abreviar(actor.getNome()) : actor.getNome();
        eventos.publishEvent(new AvaliacaoRecebida(avaliado, avaliado.getNome(), avaliado.getEmail(),
                quem, autor == Avaliacao.Autor.ALUNA, request.nota(), avaliacao.getComentario()));

        return toResponse(avaliacao);
    }

    /** O que esta pessoa pode fazer com a mentoria agora, o que já avaliou e o que escreveram sobre ela. */
    @Transactional(readOnly = true)
    public AvaliacaoDaMentoriaResponse situacao(Usuario actor, Long mentoriaId) {
        Mentoria mentoria = buscar(mentoriaId);
        autorDe(actor, mentoria.getAluna(), mentoria.getHorario().getMentora().getUsuario());

        List<Avaliacao> todas = avaliacoes.findByMentoriaId(mentoriaId);

        Avaliacao minha = todas.stream()
                .filter(a -> a.getAvaliador().getId().equals(actor.getId()))
                .findFirst().orElse(null);

        // a avaliação escondida pela moderação não aparece
        Avaliacao recebida = todas.stream()
                .filter(a -> a.getAvaliado().getId().equals(actor.getId()) && !a.isOculta())
                .findFirst().orElse(null);

        String motivo = motivoDeNaoPoder(mentoria, actor.getId());

        return new AvaliacaoDaMentoriaResponse(
                mentoriaId,
                motivo == null,
                motivo,
                prazoFinal(mentoria),
                minha == null ? null : toResponse(minha),
                recebida == null ? null : toResponse(recebida));
    }

    /** As mentorias concluídas que esta pessoa ainda pode avaliar, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<AvaliacaoPendenteResponse> pendentes(Usuario actor) {
        LocalDateTime limite = LocalDateTime.now().minusDays(LembreteDeMentoriaService.DIAS_PARA_AVALIAR);

        return mentorias.pendentesDeAvaliacao(actor.getId(), Mentoria.Status.CONCLUIDA, limite).stream()
                .map(mentoria -> {
                    boolean ehAluna = mentoria.getAluna().getId().equals(actor.getId());
                    Usuario outra = ehAluna ? mentoria.getHorario().getMentora().getUsuario() : mentoria.getAluna();

                    return new AvaliacaoPendenteResponse(
                            mentoria.getId(),
                            ehAluna ? Avaliacao.Autor.ALUNA : Avaliacao.Autor.MENTORA,
                            outra.getNome(),
                            outra.getFotoUrl(),
                            mentoria.getHorario().getInicio(),
                            prazoFinal(mentoria));
                })
                .toList();
    }

    /** O que escreveram sobre esta pessoa (sem as ocultadas), da mais nova para a mais antiga. */
    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> recebidas(Usuario actor) {
        return avaliacoes.findByAvaliadoIdAndOcultaFalseOrderByCriadaEmDesc(actor.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Moderação: só a admin. Esconde (ou mostra de novo) o comentário; a nota continua valendo na média. */
    public void ocultar(Usuario actor, Long id, boolean oculta) {
        RegraAcesso.exigirAdmin(actor);
        Avaliacao avaliacao = avaliacoes.findById(id)
                .orElseThrow(() -> new NotFoundException("Avaliação não encontrada."));
        avaliacao.setOculta(oculta);
    }

    /** Perfil público: nota média, barrinhas por estrela e os comentários (com o nome da aluna abreviado). */
    @Transactional(readOnly = true)
    public AvaliacoesDaMentoraResponse daMentora(Long mentoraPerfilId, int limite) {
        PerfilMentora perfil = perfis.findById(mentoraPerfilId)
                .orElseThrow(() -> new NotFoundException("Mentora não encontrada."));
        Long usuarioId = perfil.getUsuario().getId();

        int quantidade = Math.max(1, Math.min(limite, LIMITE_MAXIMO_DA_LISTA));

        List<AvaliacaoPublicaResponse> lista = avaliacoes
                .findByAvaliadoIdAndAutorAndOcultaFalseOrderByCriadaEmDesc(
                        usuarioId, Avaliacao.Autor.ALUNA, PageRequest.of(0, quantidade))
                .stream()
                .map(a -> new AvaliacaoPublicaResponse(
                        a.getId(), abreviar(a.getAvaliador().getNome()), a.getNota(), a.getComentario(), a.getCriadaEm()))
                .toList();

        // de 5 a 1, com zero onde ninguém deu aquela nota
        Map<Integer, Long> distribuicao = new LinkedHashMap<>();
        for (int nota = 5; nota >= 1; nota--) distribuicao.put(nota, 0L);
        for (Object[] linha : avaliacoes.contarPorNota(usuarioId, Avaliacao.Autor.ALUNA)) {
            distribuicao.put(((Number) linha[0]).intValue(), ((Number) linha[1]).longValue());
        }

        return new AvaliacoesDaMentoraResponse(perfil.getNotaMedia(), perfil.getTotalAvaliacoes(), distribuicao, lista);
    }

    /**
     * A única regra de "pode avaliar agora?", usada ao avaliar e ao perguntar.
     * Devolve o motivo em texto, ou nulo se pode.
     */
    private String motivoDeNaoPoder(Mentoria mentoria, Long avaliadorId) {
        if (mentoria.getStatus() != Mentoria.Status.CONCLUIDA) {
            return "Só dá para avaliar depois que a mentoria for concluída.";
        }
        if (LocalDateTime.now().isAfter(prazoFinal(mentoria))) {
            return "O prazo para avaliar esta mentoria terminou.";
        }
        if (avaliacoes.existsByMentoriaIdAndAvaliadorId(mentoria.getId(), avaliadorId)) {
            return "Você já avaliou esta mentoria.";
        }
        return null;
    }

    private LocalDateTime prazoFinal(Mentoria mentoria) {
        return mentoria.getHorario().getFim().plusDays(LembreteDeMentoriaService.DIAS_PARA_AVALIAR);
    }

    /** Só quem participou da mentoria mexe nela, nem o admin. */
    private Avaliacao.Autor autorDe(Usuario actor, Usuario aluna, Usuario mentora) {
        if (actor.getId().equals(aluna.getId())) return Avaliacao.Autor.ALUNA;
        if (actor.getId().equals(mentora.getId())) return Avaliacao.Autor.MENTORA;
        throw new ForbiddenException();
    }

    /**
     * Média incremental: (média x total + nota) / (total + 1). Não recalcula do zero, então as notas que já
     * existiam (do seed) continuam valendo. O perfil é lido com trava para duas avaliações simultâneas não se perderem.
     */
    private void registrarNaMedia(Long mentoraUsuarioId, int nota) {
        PerfilMentora perfil = perfis.findByUsuarioIdForUpdate(mentoraUsuarioId)
                .orElseThrow(() -> new NotFoundException("Perfil de mentora não encontrado."));

        int total = perfil.getTotalAvaliacoes();
        double soma = perfil.getNotaMedia() * total + nota;

        perfil.setTotalAvaliacoes(total + 1);
        perfil.setNotaMedia(Math.round(soma / (total + 1) * 100.0) / 100.0);
    }

    /** "Daniela Martins" vira "Daniela M."; nome de uma palavra só fica como está. */
    static String abreviar(String nome) {
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) return partes[0];
        return partes[0] + " " + Character.toUpperCase(partes[partes.length - 1].charAt(0)) + ".";
    }

    private static String limpar(String comentario) {
        if (comentario == null) return null;
        String limpo = comentario.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private Mentoria buscar(Long id) {
        return mentorias.findById(id).orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));
    }

    private AvaliacaoResponse toResponse(Avaliacao a) {
        return new AvaliacaoResponse(a.getId(), a.getMentoria().getId(), a.getAutor(),
                a.getAvaliador().getNome(), a.getNota(), a.getComentario(), a.getCriadaEm());
    }
}