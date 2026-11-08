package com.techmentor.api;

import com.techmentor.api.dto.MentoriaDtos.AgendamentoRequest;
import com.techmentor.api.dto.MentoriaDtos.AgendamentoResponse;
import com.techmentor.api.dto.MentoriaDtos.HorarioRequest;
import com.techmentor.api.dto.MentoriaDtos.HorarioResponse;
import com.techmentor.api.dto.MentoriaDtos.MentoraRequest;
import com.techmentor.api.dto.MentoriaDtos.MentoraResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.AgendamentoService;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Quem pode ver, cancelar, reagendar e confirmar um agendamento: só os participantes (e a admin). */
@SpringBootTest
@Transactional
class AgendamentoAcessoTest {

    @Autowired
    private MentoraService mentoras;

    @Autowired
    private AgendamentoService agendamentos;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario aluna;
    private Usuario intrusa;
    private Usuario mentora;
    private Usuario outraMentora;
    private Usuario admin;

    private AgendamentoResponse agendamento;
    private HorarioResponse outroHorario;

    private Usuario criarUsuario(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail("t" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private MentoraResponse criarPerfil(Usuario dona) {
        return mentoras.criarMentora(dona,
                new MentoraRequest("Bio de teste com mais de dez letras.", "Engenheira", "Empresa X", 5, Set.of()));
    }

    private HorarioResponse criarHorario(Usuario dona, MentoraResponse perfil, int diasAFrente) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(diasAFrente).withHour(14).withMinute(0).withSecond(0).withNano(0);
        return mentoras.criarHorario(dona, perfil.id(), new HorarioRequest(inicio, inicio.plusHours(1)));
    }

    private static void assertProibido(Executable acao) {
        ApiException erro = assertThrows(ApiException.class, acao);
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
    }

    @BeforeEach
    void preparar() {
        aluna = criarUsuario(Usuario.Tipo.ALUNA);
        intrusa = criarUsuario(Usuario.Tipo.ALUNA);
        mentora = criarUsuario(Usuario.Tipo.MENTORA);
        outraMentora = criarUsuario(Usuario.Tipo.MENTORA);
        admin = criarUsuario(Usuario.Tipo.ADMIN);

        MentoraResponse perfil = criarPerfil(mentora);
        criarPerfil(outraMentora);

        HorarioResponse horario = criarHorario(mentora, perfil, 3);
        outroHorario = criarHorario(mentora, perfil, 4);

        agendamento = agendamentos.criar(aluna, new AgendamentoRequest(horario.id(), "Dúvidas de carreira"));
    }

    @Test
    void outraAlunaNaoVeNemCancelaNemReagendaAgendamentoAlheio() {
        assertProibido(() -> agendamentos.obter(intrusa, agendamento.id()));
        assertProibido(() -> agendamentos.cancelar(intrusa, agendamento.id()));
        assertProibido(() -> agendamentos.reagendar(intrusa, agendamento.id(), outroHorario.id()));
    }

    @Test
    void outraMentoraNaoMexeNoAgendamento() {
        assertProibido(() -> agendamentos.obter(outraMentora, agendamento.id()));
        assertProibido(() -> agendamentos.cancelar(outraMentora, agendamento.id()));
        assertProibido(() -> agendamentos.confirmar(outraMentora, agendamento.id()));
    }

    @Test
    void soAMentoraDoAgendamentoConfirma() {
        assertProibido(() -> agendamentos.confirmar(aluna, agendamento.id()));
        assertProibido(() -> agendamentos.confirmar(intrusa, agendamento.id()));

        assertDoesNotThrow(() -> agendamentos.confirmar(mentora, agendamento.id()));
    }

    @Test
    void participantesEAdminVeemOAgendamento() {
        assertDoesNotThrow(() -> agendamentos.obter(aluna, agendamento.id()));
        assertDoesNotThrow(() -> agendamentos.obter(mentora, agendamento.id()));
        assertDoesNotThrow(() -> agendamentos.obter(admin, agendamento.id()));
    }
}