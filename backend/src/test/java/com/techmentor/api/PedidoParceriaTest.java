package com.techmentor.api;

import com.techmentor.api.dto.PedidoParceriaDtos.PedidoRequest;
import com.techmentor.api.dto.PedidoParceriaDtos.PedidoResponse;
import com.techmentor.api.evento.EventosDeNotificacao.ParceiraConvidada;
import com.techmentor.api.evento.EventosDeNotificacao.PedidoDeParceriaRecebido;
import com.techmentor.api.evento.EventosDeNotificacao.PedidoDeParceriaRecusado;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.AcaoAuditada;
import com.techmentor.api.model.DestinoNotificacao;
import com.techmentor.api.model.RegistroAuditoria;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.SituacaoPedido;
import com.techmentor.api.model.TipoParceira;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.NotificacaoRepository;
import com.techmentor.api.repository.ParceiraRepository;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.AuditoriaService;
import com.techmentor.api.service.PedidoParceriaService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O pedido de parceria feito pelo site: entra na fila, a equipe é avisada, a administradora aprova ou recusa. */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class PedidoParceriaTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Autowired private PedidoParceriaService pedidos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ParceiraRepository parceiras;
    @Autowired private NotificacaoRepository notificacoes;
    @Autowired private AuditoriaService auditoria;
    @Autowired private ApplicationEvents eventos;

    // ---------------------------------------------------------------- apoio

    private static String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario criar(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail("t" + sufixo() + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private static PedidoRequest pedido(String nome, String email) {
        return new PedidoRequest(nome, TipoParceira.ONG, null, "https://www.instituto-teste.org", "  Marina Duarte  ", email,
                "Temos doze cursos de lógica e web em vídeo, gratuitos.", true, "2026-11-18", null);
    }

    private static PedidoRequest novo() {
        return pedido("Instituto " + sufixo(), "contato" + sufixo() + "@instituto-teste.org");
    }

    private PedidoResponse pendenteDeUmPedidoNovo(Usuario admin) {
        pedidos.registrar(novo());
        return pedidos.listar(admin, SituacaoPedido.PENDENTE).get(0);
    }

    // ---------------------------------------------------------------- pedir (rota pública)

    @Test
    void oPedidoEntraNaFilaEAEquipeEhAvisada() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoRequest dados = pedido("Instituto " + sufixo(), "  CONTATO" + sufixo() + "@Instituto-Teste.org ");

        pedidos.registrar(dados);

        List<PedidoResponse> fila = pedidos.listar(admin, SituacaoPedido.PENDENTE);
        PedidoResponse achado = fila.stream().filter(p -> p.nome().equals(dados.nome())).findFirst().orElseThrow();
        assertEquals("Marina Duarte", achado.responsavel());                       // sem os espaços das pontas
        assertEquals(dados.email().trim().toLowerCase(), achado.email());          // e-mail em minúsculas
        assertEquals(SituacaoPedido.PENDENTE, achado.situacao());
        assertEquals("2026-11-18", achado.politicaVersao());                       // prova do consentimento
        assertNotNull(achado.consentimentoEm());

        assertTrue(eventos.stream(PedidoDeParceriaRecebido.class).anyMatch(e -> e.pedidoId().equals(achado.id())),
                "sai e-mail de confirmação e aviso para a equipe");

        // sininho da administradora, levando ao pedido
        assertTrue(notificacoes.findByUsuarioOrderByCriadaEmDesc(admin).stream().anyMatch(n ->
                n.getDestino() == DestinoNotificacao.PEDIDO_DE_PARCERIA && String.valueOf(achado.id()).equals(n.getReferencia())));

        // e o histórico registra o pedido
        assertTrue(auditoria.historico(RegistroAuditoria.PEDIDO, achado.id()).stream()
                .anyMatch(r -> r.acao() == AcaoAuditada.PEDIDO_RECEBIDO));
    }

    @Test
    void campoEscondidoPreenchidoEhRoboEFingeQueDeuCerto() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        String nome = "Instituto " + sufixo();
        PedidoRequest robo = new PedidoRequest(nome, TipoParceira.ONG, null, null, "Robô", "robo" + sufixo() + "@spam.com",
                "Compre agora o nosso produto incrível!!", true, "2026-11-18", "https://spam.example");

        pedidos.registrar(robo);   // não dá erro, para o robô não aprender nada

        assertTrue(pedidos.listar(admin, null).stream().noneMatch(p -> p.nome().equals(nome)));
        assertEquals(0, eventos.stream(PedidoDeParceriaRecebido.class).count());
    }

    @Test
    void pedidoRepetidoEmailQueJaTemContaOuParceiraExistenteNaoCriamNada() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoRequest dados = novo();
        pedidos.registrar(dados);
        long antes = pedidos.listar(admin, null).size();

        pedidos.registrar(dados);                                                       // o mesmo, ainda pendente
        pedidos.registrar(pedido("Outro " + sufixo(), dados.email()));                  // mesmo e-mail
        Usuario comConta = criar(Usuario.Tipo.ALUNA);
        pedidos.registrar(pedido("Mais um " + sufixo(), comConta.getEmail()));          // e-mail de quem já tem conta

        assertEquals(antes, pedidos.listar(admin, null).size());
    }

    @Test
    void dadosInvalidosSaoRecusadosComMensagemClara() {
        // empresa sem CNPJ, CNPJ que não existe, e a política não aceita
        assertThrows(BadRequestException.class, () -> pedidos.registrar(new PedidoRequest("Empresa " + sufixo(),
                TipoParceira.EMPRESA, null, null, "Fulana", "e" + sufixo() + "@empresa.com",
                "Vendemos cursos de tecnologia para iniciantes.", true, "2026-11-18", null)));
        assertThrows(BadRequestException.class, () -> pedidos.registrar(new PedidoRequest("Empresa " + sufixo(),
                TipoParceira.EMPRESA, "11.222.333/0001-82", null, "Fulana", "e" + sufixo() + "@empresa.com",
                "Vendemos cursos de tecnologia para iniciantes.", true, "2026-11-18", null)));
        assertThrows(BadRequestException.class, () -> pedidos.registrar(new PedidoRequest("Instituto " + sufixo(),
                TipoParceira.ONG, null, null, "Fulana", "e" + sufixo() + "@ong.org",
                "Temos cursos gratuitos de programação.", false, "2026-11-18", null)));
    }

    @Test
    void oFormularioValidaOsCamposAntesDeChegarAoServico() {
        assertTrue(VALIDADOR.validate(novo()).isEmpty());

        assertFalse(VALIDADOR.validate(new PedidoRequest("", TipoParceira.ONG, null, null, "Ana", "a@b.com",
                "Mensagem com mais de vinte caracteres.", true, "v1", null)).isEmpty());
        assertFalse(VALIDADOR.validate(new PedidoRequest("Instituto", TipoParceira.ONG, null, null, "Ana", "isso-nao-e-email",
                "Mensagem com mais de vinte caracteres.", true, "v1", null)).isEmpty());
        assertFalse(VALIDADOR.validate(new PedidoRequest("Instituto", TipoParceira.ONG, null, "javascript:alert(1)", "Ana", "a@b.com",
                "Mensagem com mais de vinte caracteres.", true, "v1", null)).isEmpty());
        assertFalse(VALIDADOR.validate(new PedidoRequest("Instituto", TipoParceira.ONG, null, null, "Ana", "a@b.com",
                "curta", true, "v1", null)).isEmpty());
        assertFalse(VALIDADOR.validate(new PedidoRequest("Instituto", null, null, null, "Ana", "a@b.com",
                "Mensagem com mais de vinte caracteres.", true, "v1", null)).isEmpty());
    }

    // ---------------------------------------------------------------- aprovar

    @Test
    void aprovarCriaAParceiraConvidadaEMandaOConvite() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse fila = pendenteDeUmPedidoNovo(admin);

        PedidoResponse aprovado = pedidos.aprovar(admin, fila.id());

        assertEquals(SituacaoPedido.APROVADO, aprovado.situacao());
        assertNotNull(aprovado.parceiraId());
        assertEquals(admin.getNome(), aprovado.decididoPor());
        assertNotNull(aprovado.decididoEm());

        var parceira = parceiras.findById(aprovado.parceiraId()).orElseThrow();
        assertEquals(fila.nome(), parceira.getNome());
        assertEquals(SituacaoParceira.CONVIDADA, parceira.getSituacao());
        assertEquals(Usuario.Tipo.PARCEIRA, parceira.getUsuario().getTipo());
        assertFalse(parceira.getUsuario().isAtivo(), "a conta só abre quando ela aceitar o convite");

        // o e-mail de convite (código e link) é disparado
        assertTrue(eventos.stream(ParceiraConvidada.class).anyMatch(e -> e.email().equals(fila.email())));

        // e não aparece mais como pendente
        assertTrue(pedidos.listar(admin, SituacaoPedido.PENDENTE).stream().noneMatch(p -> p.id().equals(fila.id())));
        assertTrue(auditoria.historico(RegistroAuditoria.PEDIDO, fila.id()).stream()
                .anyMatch(r -> r.acao() == AcaoAuditada.PEDIDO_APROVADO && r.autorId().equals(admin.getId())));
    }

    @Test
    void pedidoJaRespondidoNaoSeAprovaNemSeRecusaDeNovo() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse fila = pendenteDeUmPedidoNovo(admin);
        pedidos.aprovar(admin, fila.id());

        assertThrows(ConflictException.class, () -> pedidos.aprovar(admin, fila.id()));
        assertThrows(ConflictException.class, () -> pedidos.recusar(admin, fila.id(), "Tarde demais."));
    }

    @Test
    void seOEmailVirouContaNoMeioDoCaminhoAprovarRecusaEOPedidoContinuaPendente() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse fila = pendenteDeUmPedidoNovo(admin);

        Usuario intruso = new Usuario();
        intruso.setNome("Outra pessoa");
        intruso.setEmail(fila.email());
        intruso.setSenhaHash("x");
        usuarios.save(intruso);

        assertThrows(ConflictException.class, () -> pedidos.aprovar(admin, fila.id()));
        assertEquals(SituacaoPedido.PENDENTE, pedidos.detalhar(admin, fila.id()).situacao());
    }

    // ---------------------------------------------------------------- recusar

    @Test
    void recusarGuardaOMotivoEAvisaAInstituicao() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse fila = pendenteDeUmPedidoNovo(admin);

        PedidoResponse recusado = pedidos.recusar(admin, fila.id(), "  Por enquanto só aceitamos cursos em português.  ");

        assertEquals(SituacaoPedido.RECUSADO, recusado.situacao());
        assertEquals("Por enquanto só aceitamos cursos em português.", recusado.motivoDaRecusa());
        assertNull(recusado.parceiraId());
        assertTrue(eventos.stream(PedidoDeParceriaRecusado.class)
                .anyMatch(e -> e.email().equals(fila.email()) && e.motivo().startsWith("Por enquanto")));
        assertTrue(auditoria.historico(RegistroAuditoria.PEDIDO, fila.id()).stream()
                .anyMatch(r -> r.acao() == AcaoAuditada.PEDIDO_RECUSADO));
    }

    // ---------------------------------------------------------------- apagar (LGPD)

    @Test
    void apagarRemoveOsDadosDoPedidoEODeixaSoONumeroNoHistorico() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse fila = pendenteDeUmPedidoNovo(admin);

        pedidos.apagar(admin, fila.id());

        assertThrows(NotFoundException.class, () -> pedidos.detalhar(admin, fila.id()));
        assertTrue(pedidos.listar(admin, null).stream().noneMatch(p -> p.id().equals(fila.id())));
        var historico = auditoria.historico(RegistroAuditoria.PEDIDO, fila.id());
        assertTrue(historico.stream().anyMatch(r -> r.acao() == AcaoAuditada.PEDIDO_APAGADO));
        assertTrue(historico.stream().noneMatch(r -> String.valueOf(r.detalhe()).contains(fila.email())),
                "o histórico não guarda dado pessoal");
    }

    @Test
    void pedidoRecusadoTambemSeApagaMasOAprovadoNao() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse recusado = pendenteDeUmPedidoNovo(admin);
        pedidos.recusar(admin, recusado.id(), "Fora do perfil.");
        pedidos.apagar(admin, recusado.id());
        assertThrows(NotFoundException.class, () -> pedidos.detalhar(admin, recusado.id()));

        PedidoResponse aprovado = pendenteDeUmPedidoNovo(admin);
        pedidos.aprovar(admin, aprovado.id());
        assertThrows(ConflictException.class, () -> pedidos.apagar(admin, aprovado.id()));
        assertEquals(SituacaoPedido.APROVADO, pedidos.detalhar(admin, aprovado.id()).situacao());
    }

    @Test
    void soAAdministradoraApagaEPedidoQueNaoExisteDa404() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse fila = pendenteDeUmPedidoNovo(admin);
        Usuario mentora = criar(Usuario.Tipo.MENTORA);

        assertThrows(ForbiddenException.class, () -> pedidos.apagar(mentora, fila.id()));
        assertThrows(NotFoundException.class, () -> pedidos.apagar(admin, 999_999L));
    }

    // ---------------------------------------------------------------- quem pode

    @Test
    void soAAdministradoraVeEDecide() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        PedidoResponse fila = pendenteDeUmPedidoNovo(admin);
        Usuario aluna = criar(Usuario.Tipo.ALUNA);
        Usuario mentora = criar(Usuario.Tipo.MENTORA);

        for (Usuario intrusa : List.of(aluna, mentora)) {
            assertThrows(ForbiddenException.class, () -> pedidos.listar(intrusa, null));
            assertThrows(ForbiddenException.class, () -> pedidos.detalhar(intrusa, fila.id()));
            assertThrows(ForbiddenException.class, () -> pedidos.aprovar(intrusa, fila.id()));
            assertThrows(ForbiddenException.class, () -> pedidos.recusar(intrusa, fila.id(), "Não."));
        }
        assertThrows(NotFoundException.class, () -> pedidos.detalhar(admin, 999_999L));
    }

    @Test
    void aPistaDoDominioAjudaAAnalise() {
        Usuario admin = criar(Usuario.Tipo.ADMIN);
        pedidos.registrar(new PedidoRequest("Instituto " + sufixo(), TipoParceira.ONG, null, "https://www.instituto-pista.org.br",
                "Ana", "ana@instituto-pista.org.br", "Cursos gratuitos de lógica e dados, em vídeo.", true, "v1", null));
        pedidos.registrar(new PedidoRequest("Outro " + sufixo(), TipoParceira.ONG, null, "https://www.outro-pista.org.br",
                "Bia", "bia" + sufixo() + "@gmail.com", "Cursos gratuitos de design, em vídeo.", true, "v1", null));

        List<PedidoResponse> fila = pedidos.listar(admin, SituacaoPedido.PENDENTE);

        assertTrue(fila.stream().anyMatch(p -> p.email().equals("ana@instituto-pista.org.br")
                && Boolean.TRUE.equals(p.dominioDoEmailConfereComSite())));
        assertTrue(fila.stream().anyMatch(p -> p.email().endsWith("@gmail.com")
                && Boolean.FALSE.equals(p.dominioDoEmailConfereComSite())));
    }
}
