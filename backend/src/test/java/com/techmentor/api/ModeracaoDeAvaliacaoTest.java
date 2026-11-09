package com.techmentor.api;

import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoRequest;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoResponse;
import com.techmentor.api.dto.MentoriaDtos.AgendamentoRequest;
import com.techmentor.api.dto.MentoriaDtos.AgendamentoResponse;
import com.techmentor.api.dto.MentoriaDtos.HorarioRequest;
import com.techmentor.api.dto.MentoriaDtos.HorarioResponse;
import com.techmentor.api.dto.MentoriaDtos.MentoraRequest;
import com.techmentor.api.dto.MentoriaDtos.MentoraResponse;
import com.techmentor.api.dto.SegurancaDtos.DenunciaRequest;
import com.techmentor.api.dto.SegurancaDtos.DenunciaResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.HorarioDisponivel;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.HorarioDisponivelRepository;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.AgendamentoService;
import com.techmentor.api.service.AvaliacaoService;
import com.techmentor.api.service.DenunciaService;
import com.techmentor.api.service.LembreteDeMentoriaService;
import com.techmentor.api.service.MentoraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Denunciar o comentário de uma avaliação, a admin esconder, e o prazo de 14 dias para avaliar. */
@SpringBootTest
@Transactional
class ModeracaoDeAvaliacaoTest {

    private static final String COMENTARIO = "Foi grosseira e não ajudou em nada.";

    @Autowired private MentoraService mentoras;
    @Autowired private AgendamentoService agendamentos;
    @Autowired private AvaliacaoService avaliacoes;
    @Autowired private DenunciaService denuncias;
    @Autowired private LembreteDeMentoriaService lembretes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private HorarioDisponivelRepository horarios;

    private Usuario aluna;
    private Usuario outraAluna;
    private Usuario mentora;
    private Usuario admin;
    private MentoraResponse perfil;

    private Usuario criarUsuario(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail("t" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    @BeforeEach
    void preparar() {
        aluna = criarUsuario(Usuario.Tipo.ALUNA);
        outraAluna = criarUsuario(Usuario.Tipo.ALUNA);
        mentora = criarUsuario(Usuario.Tipo.MENTORA);
        admin = criarUsuario(Usuario.Tipo.ADMIN);
        perfil = mentoras.criarMentora(mentora,
                new MentoraRequest("Bio de teste com mais de dez letras.", "Engenheira", "Empresa X", 5, Set.of()));
    }

    /** Agenda e a mentora confirma. A mentoria ainda está no futuro (CONFIRMADA). */
    private Long mentoriaConfirmada(int diasAFrente) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(diasAFrente).withHour(14).withMinute(0).withSecond(0).withNano(0);
        HorarioResponse horario = mentoras.criarHorario(mentora, perfil.id(), new HorarioRequest(inicio, inicio.plusHours(1)));
        AgendamentoResponse agendamento = agendamentos.criar(aluna, new AgendamentoRequest(horario.id(), "Dúvidas de carreira"));
        agendamentos.confirmar(mentora, agendamento.id());
        return agendamento.id();
    }

    /** Faz a mentoria terminar no momento pedido e deixa o relógio concluí-la, como em produção. */
    private Long mentoriaConcluida(int diasAFrente, LocalDateTime fim) {
        Long id = mentoriaConfirmada(diasAFrente);
        HorarioDisponivel horario = horarios.findAll().stream()
                .filter(h -> h.getMentora().getId().equals(perfil.id()))
                .filter(h -> h.getInicio().toLocalDate().equals(LocalDateTime.now().plusDays(diasAFrente).toLocalDate()))
                .findFirst().orElseThrow();
        horario.setFim(fim);
        horario.setInicio(fim.minusHours(1));
        horarios.save(horario);
        lembretes.processar();
        return id;
    }

    private static HttpStatus statusDe(Executable acao) {
        return assertThrows(ApiException.class, acao).getStatus();
    }

    private AvaliacaoResponse alunaAvaliaComComentario(Long mentoriaId) {
        return avaliacoes.avaliar(aluna, mentoriaId, new AvaliacaoRequest(1, COMENTARIO));
    }

    private DenunciaResponse mentoraDenuncia(Long avaliacaoId) {
        return denuncias.criar(mentora, new DenunciaRequest(null, Denuncia.Motivo.COMPORTAMENTO_INADEQUADO,
                "Comentário ofensivo e sem respeito", avaliacaoId));
    }

    @Test
    void fluxoCompletoDaDenunciaAteEsconderOComentario() {
        Long mentoriaId = mentoriaConcluida(3, LocalDateTime.now().minusHours(1));
        AvaliacaoResponse avaliacao = alunaAvaliaComComentario(mentoriaId);
        assertEquals(1, avaliacoes.recebidas(mentora).size());

        // 1) a mentora denuncia o comentário; o denunciado é quem escreveu, escolhido pelo servidor
        DenunciaResponse denuncia = mentoraDenuncia(avaliacao.id());
        assertEquals(avaliacao.id(), denuncia.avaliacaoId());
        assertEquals(aluna.getId(), denuncia.denunciadoId());
        assertEquals(COMENTARIO, denuncia.avaliacaoComentario());
        assertEquals(Boolean.FALSE, denuncia.avaliacaoOculta());

        // 2) a admin vê o comentário dentro da denúncia e esconde
        assertEquals(COMENTARIO, denuncias.obter(admin, denuncia.id()).avaliacaoComentario());
        avaliacoes.ocultar(admin, avaliacao.id(), true);

        // 3) some para a mentora e para o público, mas a nota continua na média
        assertTrue(avaliacoes.recebidas(mentora).isEmpty());
        var publica = avaliacoes.daMentora(perfil.id(), 20);
        assertTrue(publica.avaliacoes().isEmpty());
        assertEquals(1.0, publica.notaMedia());
        assertEquals(Boolean.TRUE, denuncias.obter(admin, denuncia.id()).avaliacaoOculta());

        // 4) a admin pode desfazer
        avaliacoes.ocultar(admin, avaliacao.id(), false);
        assertEquals(1, avaliacoes.recebidas(mentora).size());
    }

    @Test
    void soQuemFoiAvaliadaDenunciaOComentario() {
        Long mentoriaId = mentoriaConcluida(3, LocalDateTime.now().minusHours(1));
        AvaliacaoResponse avaliacao = alunaAvaliaComComentario(mentoriaId);

        // terceira pessoa e a própria autora não podem
        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> denuncias.criar(outraAluna,
                new DenunciaRequest(null, Denuncia.Motivo.OUTRO, "Quero denunciar este comentário", avaliacao.id()))));
        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> denuncias.criar(aluna,
                new DenunciaRequest(null, Denuncia.Motivo.OUTRO, "Quero denunciar o meu comentário", avaliacao.id()))));
    }

    @Test
    void naoDenunciaDuasVezesNemAvaliacaoSemComentario() {
        Long mentoriaId = mentoriaConcluida(3, LocalDateTime.now().minusHours(1));
        AvaliacaoResponse comComentario = alunaAvaliaComComentario(mentoriaId);

        mentoraDenuncia(comComentario.id());
        assertEquals(HttpStatus.CONFLICT, statusDe(() -> mentoraDenuncia(comComentario.id())));

        // a mentora avalia a aluna sem escrever nada: não há comentário para denunciar
        AvaliacaoResponse semComentario = avaliacoes.avaliar(mentora, mentoriaId, new AvaliacaoRequest(5, null));
        assertEquals(HttpStatus.BAD_REQUEST, statusDe(() -> denuncias.criar(aluna,
                new DenunciaRequest(null, Denuncia.Motivo.OUTRO, "Quero denunciar esta avaliação", semComentario.id()))));
    }

    @Test
    void soAdminEscondeEAAvaliacaoPrecisaExistir() {
        Long mentoriaId = mentoriaConcluida(3, LocalDateTime.now().minusHours(1));
        AvaliacaoResponse avaliacao = alunaAvaliaComComentario(mentoriaId);

        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> avaliacoes.ocultar(mentora, avaliacao.id(), true)));
        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> avaliacoes.ocultar(aluna, avaliacao.id(), true)));
        assertEquals(HttpStatus.NOT_FOUND, statusDe(() -> avaliacoes.ocultar(admin, 999_999L, true)));
    }

    @Test
    void prazoDeQuatorzeDiasParaAvaliar() {
        // terminou há 13 dias: ainda dá tempo
        Long dentro = mentoriaConcluida(3, LocalDateTime.now().minusDays(13));
        assertNotNull(avaliacoes.avaliar(aluna, dentro, new AvaliacaoRequest(5, "Ótima conversa")));
    }

    @Test
    void passadoOPrazoNaoSeAvalia() {
        // terminou há 15 dias: o prazo acabou
        Long fora = mentoriaConcluida(3, LocalDateTime.now().minusDays(15));
        ApiException erro = assertThrows(ApiException.class,
                () -> avaliacoes.avaliar(aluna, fora, new AvaliacaoRequest(5, "Tarde demais")));
        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        assertTrue(erro.getMessage().contains("prazo"), erro.getMessage());
    }

    @Test
    void antesDeConcluirNaoSeAvalia() {
        Long futura = mentoriaConfirmada(6);
        ApiException erro = assertThrows(ApiException.class,
                () -> avaliacoes.avaliar(aluna, futura, new AvaliacaoRequest(5, "Cedo demais")));
        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        assertFalse(erro.getMessage().isBlank());
    }
}