package com.techmentor.api;

import com.techmentor.api.dto.MentoriaDtos.AgendamentoRequest;
import com.techmentor.api.dto.MentoriaDtos.AgendamentoResponse;
import com.techmentor.api.dto.MentoriaDtos.HorarioRequest;
import com.techmentor.api.dto.MentoriaDtos.HorarioResponse;
import com.techmentor.api.dto.MentoriaDtos.MentoraRequest;
import com.techmentor.api.model.HorarioDisponivel;
import com.techmentor.api.model.Mentoria;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.HorarioDisponivelRepository;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.AgendamentoService;
import com.techmentor.api.service.LembreteDeMentoriaService;
import com.techmentor.api.service.MentoraService;
import com.techmentor.api.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A mentoria que ninguém confirmou e cujo horário já começou expira sozinha. */
@SpringBootTest
@Transactional
class ExpirarMentoriaNaoConfirmadaTest {

    @Autowired
    private MentoraService mentoras;

    @Autowired
    private AgendamentoService agendamentos;

    @Autowired
    private LembreteDeMentoriaService lembretes;

    @Autowired
    private NotificacaoService notificacoes;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private HorarioDisponivelRepository horarios;

    private Usuario aluna;
    private Usuario mentora;
    private HorarioResponse horario;
    private AgendamentoResponse agendamento;

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
        mentora = criarUsuario(Usuario.Tipo.MENTORA);

        var perfil = mentoras.criarMentora(mentora,
                new MentoraRequest("Bio de teste com mais de dez letras.", "Engenheira", "Empresa X", 5, Set.of()));

        LocalDateTime inicio = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        horario = mentoras.criarHorario(mentora, perfil.id(), new HorarioRequest(inicio, inicio.plusHours(1)));

        agendamento = agendamentos.criar(aluna, new AgendamentoRequest(horario.id(), "Dúvidas de carreira"));
    }

    /** Faz o horário parecer que já passou (o criar não deixa agendar no passado). */
    private void empurrarHorarioParaOPassado() {
        HorarioDisponivel h = horarios.findById(horario.id()).orElseThrow();
        h.setInicio(LocalDateTime.now().minusHours(2));
        h.setFim(LocalDateTime.now().minusHours(1));
        horarios.save(h);
    }

    private Mentoria.Status statusAtual() {
        return agendamentos.obter(aluna, agendamento.id()).status();
    }

    private long quantasComTitulo(Usuario pessoa, String titulo) {
        return notificacoes.listarDTO(pessoa.getId(), null).stream()
                .filter(n -> titulo.equals(n.titulo()))
                .count();
    }

    @Test
    void agendadaQueJaComecouExpiraEAvisaAsDuasPessoas() {
        empurrarHorarioParaOPassado();

        lembretes.processar();

        assertEquals(Mentoria.Status.CANCELADA, statusAtual());
        assertEquals(1, quantasComTitulo(aluna, "Mentoria não confirmada"));
        assertEquals(1, quantasComTitulo(mentora, "Mentoria expirada"));
    }

    @Test
    void agendadaNoFuturoNaoExpira() {
        lembretes.processar();

        assertEquals(Mentoria.Status.AGENDADA, statusAtual());
        assertEquals(0, quantasComTitulo(aluna, "Mentoria não confirmada"));
    }

    @Test
    void confirmadaNaoExpira() {
        agendamentos.confirmar(mentora, agendamento.id());
        empurrarHorarioParaOPassado();

        lembretes.processar();

        // a confirmada que terminou vira CONCLUIDA, nunca CANCELADA
        assertEquals(Mentoria.Status.CONCLUIDA, statusAtual());
        assertEquals(0, quantasComTitulo(aluna, "Mentoria não confirmada"));
    }

    @Test
    void naoAvisaDuasVezes() {
        empurrarHorarioParaOPassado();

        lembretes.processar();
        lembretes.processar();

        assertEquals(1, quantasComTitulo(aluna, "Mentoria não confirmada"));
        assertEquals(1, quantasComTitulo(mentora, "Mentoria expirada"));
    }
}