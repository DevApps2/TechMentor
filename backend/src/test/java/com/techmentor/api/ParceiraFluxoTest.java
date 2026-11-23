package com.techmentor.api;

import com.techmentor.api.dto.AuditoriaDtos.RegistroResponse;
import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import com.techmentor.api.dto.GestaoCursoDtos.AulaGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.ModuloGestaoRequest;
import com.techmentor.api.dto.ParceiraDtos.AceiteRequest;
import com.techmentor.api.dto.ParceiraDtos.ConviteRequest;
import com.techmentor.api.dto.ParceiraDtos.ParceiraResponse;
import com.techmentor.api.dto.ParceiraDtos.VerificacaoRequest;
import com.techmentor.api.evento.EventosDeNotificacao.ParceiraConvidada;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.AcaoAuditada;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.DestinoNotificacao;
import com.techmentor.api.model.Notificacao;
import com.techmentor.api.model.RegistroAuditoria;
import com.techmentor.api.model.SituacaoCurso;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.TipoNotificacao;
import com.techmentor.api.model.TipoParceira;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.ConviteParceiraRepository;
import com.techmentor.api.repository.CursoRepository;
import com.techmentor.api.repository.NotificacaoRepository;
import com.techmentor.api.repository.ParceiraRepository;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.AuditoriaService;
import com.techmentor.api.service.CursoService;
import com.techmentor.api.service.GestaoCursoService;
import com.techmentor.api.service.ParceiraCursoService;
import com.techmentor.api.service.ParceiraService;
import com.techmentor.api.service.TermosDeParceria;
import com.techmentor.api.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

/** O fluxo da empresa parceira: convite, aceite, verificação, curso em revisão, decisão da administradora e histórico. */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class ParceiraFluxoTest {

    private static final String VIDEO = "https://www.youtube.com/watch?v=sTX0UEplF54";
    private static final String CNPJ_VALIDO = "11.222.333/0001-81";
    private static final String SENHA = "senhaDaParceira1";

    @Autowired private ParceiraService parceiras;
    @Autowired private ParceiraCursoService cursosDaParceira;
    @Autowired private GestaoCursoService gestao;
    @Autowired private AuditoriaService auditoria;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CursoRepository cursos;
    @Autowired private NotificacaoRepository notificacoes;
    @Autowired private ApplicationEvents eventos;
    @Autowired private CursoService cursoService;
    @Autowired private ParceiraRepository perfis;
    @Autowired private ConviteParceiraRepository convitesRepo;

    @AfterEach
    void limparLogin() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- apoio

    private static String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario administradora() {
        Usuario u = new Usuario();
        u.setNome("Admin " + sufixo());
        u.setEmail("adm" + sufixo() + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(Usuario.Tipo.ADMIN);
        return usuarios.save(u);
    }

    private static ConviteRequest convite(String nome, String email) {
        return new ConviteRequest(nome, TipoParceira.ONG, null, null, "Responsável " + nome, email);
    }

    private static String email() {
        return "contato" + sufixo() + "@ong-teste.org";
    }

    private String ultimoCodigo() {
        return eventos.stream(ParceiraConvidada.class).reduce((a, b) -> b).orElseThrow().codigo();
    }

    private ParceiraResponse convidar(Usuario admin, String nome, String email) {
        return parceiras.convidar(admin, convite(nome, email));
    }

    private ParceiraResponse aceitar(String email, String codigo) {
        return parceiras.aceitarConvite(new AceiteRequest(email, codigo, SENHA, true, TermosDeParceria.VERSAO));
    }

    private static VerificacaoRequest checklistCompleto() {
        return new VerificacaoRequest(true, true, true, "Site e CNPJ conferidos.");
    }

    /** Uma parceira convidada, que aceitou, e já verificada: pronta para criar cursos. Devolve a conta de acesso. */
    private Usuario parceiraAtiva(Usuario admin, String nome) {
        String email = email();
        ParceiraResponse criada = convidar(admin, nome, email);
        aceitar(email, ultimoCodigo());
        parceiras.verificar(admin, criada.id(), checklistCompleto());
        return usuarios.findByEmail(email).orElseThrow();
    }

    private static CursoRequest dadosDoCurso(String titulo, String instituicao) {
        return new CursoRequest(titulo, "Descrição do curso.", Curso.Categoria.PROGRAMACAO, null, null,
                Curso.Nivel.INICIANTE, instituicao, null);
    }

    /** Um curso da parceira, com um módulo e uma aula: completo para enviar à revisão. */
    private CursoResponse cursoCompleto(Usuario parceira) {
        CursoResponse curso = cursosDaParceira.criar(parceira, dadosDoCurso("Curso " + sufixo(), null));
        ModuloResponse modulo = cursosDaParceira.criarModulo(parceira, curso.id(), new ModuloGestaoRequest("Módulo 1"));
        cursosDaParceira.criarAula(parceira, modulo.id(), new AulaGestaoRequest("Aula 1", 10, null, VIDEO));
        return gestao.detalhar(curso.id());
    }

    private List<Notificacao> notificacoesDe(Usuario usuario) {
        return notificacoes.findByUsuarioOrderByCriadaEmDesc(usuario);
    }

    private Notificacao aviso(Usuario usuario, DestinoNotificacao destino, String referencia) {
        return notificacoesDe(usuario).stream()
                .filter(n -> n.getDestino() == destino && (referencia == null || referencia.equals(n.getReferencia())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("sem notificação " + destino + " para " + usuario.getNome()));
    }

    private void logarComo(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    // ---------------------------------------------------------------- convite

    @Test
    void oConviteCriaUmaContaInativaDeParceiraEMandaOCodigo() {
        Usuario admin = administradora();
        String email = "  " + email().toUpperCase() + " ";

        ParceiraResponse criada = convidar(admin, "  Instituto " + sufixo() + " ", email);

        assertEquals(SituacaoParceira.CONVIDADA, criada.situacao());
        assertFalse(criada.aceitouOsTermos());

        Usuario conta = usuarios.findById(criada.usuarioId()).orElseThrow();
        assertEquals(Usuario.Tipo.PARCEIRA, conta.getTipo());
        assertFalse(conta.isAtivo(), "a conta só abre depois do aceite");
        assertEquals(email.trim().toLowerCase(), conta.getEmail());

        // o código vai por e-mail (aqui, no evento) e tem o formato XXXX-XXXX
        assertTrue(ultimoCodigo().matches("[A-Z2-9]{4}-[A-Z2-9]{4}"), ultimoCodigo());
    }

    @Test
    void empresaPrecisaDeCnpjValidoEOngPodeEntrarSemCnpj() {
        Usuario admin = administradora();

        assertThrows(BadRequestException.class, () -> parceiras.convidar(admin,
                new ConviteRequest("Empresa " + sufixo(), TipoParceira.EMPRESA, null, null, "Fulana", email())));
        assertThrows(BadRequestException.class, () -> parceiras.convidar(admin,
                new ConviteRequest("Empresa " + sufixo(), TipoParceira.EMPRESA, "11.222.333/0001-82", null, "Fulana", email())));
        assertThrows(BadRequestException.class, () -> parceiras.convidar(admin,
                new ConviteRequest("Empresa " + sufixo(), TipoParceira.EMPRESA, "00000000000000", null, "Fulana", email())));

        ParceiraResponse empresa = parceiras.convidar(admin,
                new ConviteRequest("Empresa " + sufixo(), TipoParceira.EMPRESA, CNPJ_VALIDO, "https://empresa.com.br", "Fulana", email()));
        assertEquals("11222333000181", empresa.documento());   // guarda só os dígitos

        ParceiraResponse ong = convidar(admin, "Ong " + sufixo(), email());
        assertNull(ong.documento());

        // o mesmo CNPJ não entra duas vezes
        assertThrows(ConflictException.class, () -> parceiras.convidar(admin,
                new ConviteRequest("Outra " + sufixo(), TipoParceira.EMPRESA, "11222333000181", null, "Fulana", email())));
    }

    @Test
    void emailENomeJaUsadosSaoRecusados() {
        Usuario admin = administradora();
        String email = email();
        String nome = "Instituto " + sufixo();
        convidar(admin, nome, email);

        assertThrows(ConflictException.class, () -> convidar(admin, "Outro " + sufixo(), email));
        assertThrows(ConflictException.class, () -> convidar(admin, nome.toUpperCase(), email()));
    }

    @Test
    void soAAdministradoraConvida() {
        Usuario aluna = new Usuario();
        aluna.setNome("Aluna");
        aluna.setEmail("al" + sufixo() + "@teste.com");
        aluna.setSenhaHash("x");
        aluna = usuarios.save(aluna);
        Usuario intrusa = aluna;

        assertThrows(ForbiddenException.class, () -> convidar(intrusa, "Instituto " + sufixo(), email()));
    }

    @Test
    void odominioDoEmailConfereComOSiteSoQuandoSaoDaMesmaInstituicao() {
        Usuario admin = administradora();

        ParceiraResponse igual = parceiras.convidar(admin, new ConviteRequest("A " + sufixo(), TipoParceira.ONG, null,
                "https://www.instituto.org.br", "Fulana", "fulana@instituto.org.br"));
        ParceiraResponse pessoal = parceiras.convidar(admin, new ConviteRequest("B " + sufixo(), TipoParceira.ONG, null,
                "https://www.instituto.org.br", "Fulana", "fulana" + sufixo() + "@gmail.com"));
        ParceiraResponse semSite = convidar(admin, "C " + sufixo(), email());

        assertEquals(Boolean.TRUE, igual.dominioDoEmailConfereComSite());
        assertEquals(Boolean.FALSE, pessoal.dominioDoEmailConfereComSite());
        assertNull(semSite.dominioDoEmailConfereComSite());
    }

    // ---------------------------------------------------------------- aceite

    @Test
    void aceitarComOCodigoCertoAbreAContaERegistraOsTermos() {
        Usuario admin = administradora();
        String email = email();
        ParceiraResponse criada = convidar(admin, "Instituto " + sufixo(), email);

        // aceita o código com letras minúsculas e sem o hífen, como a pessoa pode digitar
        String digitado = ultimoCodigo().replace("-", "").toLowerCase();
        ParceiraResponse aceita = aceitar(email, digitado);

        assertTrue(aceita.aceitouOsTermos());
        assertEquals(TermosDeParceria.VERSAO, aceita.termosVersao());
        assertNotNull(aceita.termosAceitosEm());
        assertEquals(SituacaoParceira.CONVIDADA, aceita.situacao(), "ainda falta a verificação da administradora");

        // a conta abriu e a senha escolhida por ela funciona
        Usuario entrou = usuarioService.loginUsuario(email, SENHA);
        assertEquals(Usuario.Tipo.PARCEIRA, entrou.getTipo());
        assertEquals(criada.usuarioId(), entrou.getId());

        // o código só vale uma vez
        assertThrows(BadRequestException.class, () -> aceitar(email, digitado));
    }

    @Test
    void codigoErradoContaTentativasEDepoisDeCincoBloqueia() {
        Usuario admin = administradora();
        String email = email();
        convidar(admin, "Instituto " + sufixo(), email);
        String certo = ultimoCodigo();

        for (int i = 0; i < 5; i++) {
            BadRequestException erro = assertThrows(BadRequestException.class, () -> aceitar(email, "ZZZZ-ZZZZ"));
            assertEquals("Código inválido ou expirado.", erro.getMessage());
        }

        // com as 5 tentativas gastas, nem o código certo passa mais
        BadRequestException bloqueada = assertThrows(BadRequestException.class, () -> aceitar(email, certo));
        assertTrue(bloqueada.getMessage().contains("Muitas tentativas"), bloqueada.getMessage());
    }

    @Test
    void semAceitarOsTermosOuComVersaoAntigaNaoEntra() {
        Usuario admin = administradora();
        String email = email();
        convidar(admin, "Instituto " + sufixo(), email);
        String codigo = ultimoCodigo();

        assertThrows(BadRequestException.class,
                () -> parceiras.aceitarConvite(new AceiteRequest(email, codigo, SENHA, false, TermosDeParceria.VERSAO)));
        assertThrows(BadRequestException.class,
                () -> parceiras.aceitarConvite(new AceiteRequest(email, codigo, SENHA, true, "1999-01-01")));

        // as recusas não gastaram o convite: com os termos certos, passa
        assertTrue(aceitar(email, codigo).aceitouOsTermos());
    }

    @Test
    void umaContaQueNaoEParceiraNaoAceitaConvite() {
        // e-mail de uma aluna qualquer: a resposta é a mesma de código errado
        Usuario aluna = new Usuario();
        aluna.setNome("Aluna");
        aluna.setEmail("al" + sufixo() + "@teste.com");
        aluna.setSenhaHash("x");
        Usuario salva = usuarios.save(aluna);

        assertThrows(BadRequestException.class, () -> aceitar(salva.getEmail(), "ABCD-EFGH"));
    }

    @Test
    void reenviarOConviteInvalidaOCodigoAnterior() {
        Usuario admin = administradora();
        String email = email();
        ParceiraResponse criada = convidar(admin, "Instituto " + sufixo(), email);
        String antigo = ultimoCodigo();

        // dentro do primeiro minuto o reenvio é recusado
        assertThrows(com.techmentor.api.exceptions.TooManyRequestsException.class, () -> parceiras.reenviarConvite(admin, criada.id()));

        // passado o minuto, sai um código novo e o antigo deixa de valer
        convitesRepo.findByParceiraAndUsadoFalse(perfis.findById(criada.id()).orElseThrow())
                .forEach(c -> c.setCriadoEm(java.time.LocalDateTime.now().minusMinutes(5)));
        parceiras.reenviarConvite(admin, criada.id());
        String novo = ultimoCodigo();

        assertFalse(antigo.equals(novo));
        assertThrows(BadRequestException.class, () -> aceitar(email, antigo));
        assertTrue(aceitar(email, novo).aceitouOsTermos());
    }

    // ---------------------------------------------------------------- verificação

    @Test
    void naoVerificaAntesDoAceiteNemComChecklistIncompleto() {
        Usuario admin = administradora();
        String email = email();
        ParceiraResponse criada = convidar(admin, "Instituto " + sufixo(), email);

        assertThrows(ConflictException.class, () -> parceiras.verificar(admin, criada.id(), checklistCompleto()));

        aceitar(email, ultimoCodigo());

        assertThrows(BadRequestException.class,
                () -> parceiras.verificar(admin, criada.id(), new VerificacaoRequest(true, false, true, null)));
        assertThrows(BadRequestException.class,
                () -> parceiras.verificar(admin, criada.id(), new VerificacaoRequest(false, false, false, null)));

        ParceiraResponse verificada = parceiras.verificar(admin, criada.id(), checklistCompleto());
        assertEquals(SituacaoParceira.ATIVA, verificada.situacao());
        assertTrue(verificada.verificacao().siteConferido());
        assertEquals(admin.getNome(), verificada.verificacao().verificadaPor());
        assertNotNull(verificada.verificacao().verificadaEm());
    }

    @Test
    void parceiraNaoVerificadaOuSuspensaNaoCriaCurso() {
        Usuario admin = administradora();
        String email = email();
        ParceiraResponse criada = convidar(admin, "Instituto " + sufixo(), email);
        aceitar(email, ultimoCodigo());
        Usuario conta = usuarios.findByEmail(email).orElseThrow();

        assertThrows(ForbiddenException.class, () -> cursosDaParceira.criar(conta, dadosDoCurso("Curso " + sufixo(), null)));

        parceiras.verificar(admin, criada.id(), checklistCompleto());
        CursoResponse curso = cursosDaParceira.criar(conta, dadosDoCurso("Curso " + sufixo(), null));

        ParceiraResponse suspensa = parceiras.suspender(admin, criada.id(), "Vídeos removidos do YouTube");
        assertEquals(SituacaoParceira.SUSPENSA, suspensa.situacao());
        assertEquals("Vídeos removidos do YouTube", suspensa.motivoDaSuspensao());
        assertThrows(ForbiddenException.class, () -> cursosDaParceira.criar(conta, dadosDoCurso("Outro " + sufixo(), null)));
        assertThrows(ForbiddenException.class,
                () -> cursosDaParceira.criarModulo(conta, curso.id(), new ModuloGestaoRequest("Módulo")));
        // continua podendo ler o que é dela
        assertEquals(1, cursosDaParceira.listar(conta, null).size());

        parceiras.reativar(admin, criada.id());
        assertNotNull(cursosDaParceira.criar(conta, dadosDoCurso("Outro " + sufixo(), null)).id());
    }

    // ---------------------------------------------------------------- cursos da parceira

    @Test
    void oCursoDaParceiraNasceEmRascunhoComOCreditoDela() {
        Usuario admin = administradora();
        String nome = "Instituto " + sufixo();
        Usuario conta = parceiraAtiva(admin, nome);

        // ela tenta se passar por outra instituição e tenta já nascer publicado: nada disso vale
        CursoResponse curso = cursosDaParceira.criar(conta, new CursoRequest("Curso " + sufixo(), "Descrição.",
                Curso.Categoria.DADOS, null, true, null, "Curso em Vídeo", SituacaoCurso.PUBLICADO));

        assertEquals(SituacaoCurso.RASCUNHO, curso.situacao());
        assertFalse(curso.ativo());
        assertEquals(nome, curso.instituicao());
        assertNotNull(curso.parceiraId());

        CursoResponse editado = cursosDaParceira.atualizar(conta, curso.id(),
                new CursoRequest(curso.titulo(), "Outra descrição.", Curso.Categoria.DADOS, null, null, null, "Curso em Vídeo", null));
        assertEquals(nome, editado.instituicao());
        assertEquals("Outra descrição.", editado.descricao());
    }

    @Test
    void parceiraNaoVeNemMexeNoCursoDeOutraNemNoDoTechMentor() {
        Usuario admin = administradora();
        Usuario a = parceiraAtiva(admin, "Instituto A " + sufixo());
        Usuario b = parceiraAtiva(admin, "Instituto B " + sufixo());
        CursoResponse deA = cursoCompleto(a);

        // de outra: responde "não encontrado", como se não existisse
        assertThrows(NotFoundException.class, () -> cursosDaParceira.detalhar(b, deA.id()));
        assertThrows(NotFoundException.class, () -> cursosDaParceira.atualizar(b, deA.id(), dadosDoCurso("Roubado", null)));
        assertThrows(NotFoundException.class, () -> cursosDaParceira.enviarParaRevisao(b, deA.id()));
        assertThrows(NotFoundException.class, () -> cursosDaParceira.criarModulo(b, deA.id(), new ModuloGestaoRequest("X")));
        Long moduloDeA = gestao.detalhar(deA.id()).modulos().get(0).id();
        Long aulaDeA = gestao.detalhar(deA.id()).modulos().get(0).aulas().get(0).id();
        assertThrows(NotFoundException.class, () -> cursosDaParceira.apagarModulo(b, moduloDeA));
        assertThrows(NotFoundException.class, () -> cursosDaParceira.apagarAula(b, aulaDeA));
        assertThrows(NotFoundException.class, () -> cursosDaParceira.metricas(b, deA.id()));
        assertThrows(NotFoundException.class, () -> cursosDaParceira.historico(b, deA.id()));
        assertTrue(cursosDaParceira.listar(b, null).isEmpty());

        // curso do próprio TechMentor (sem parceira): também fora do alcance
        // (o banco dos testes nasce vazio: cria um curso do próprio TechMentor, sem parceira)
        Long doTechMentor = gestao.criar(dadosDoCurso("Curso " + sufixo(), "Curso em Vídeo")).id();
        assertThrows(NotFoundException.class, () -> cursosDaParceira.detalhar(a, doTechMentor));
        assertThrows(NotFoundException.class, () -> cursosDaParceira.atualizar(a, doTechMentor, dadosDoCurso("Roubado", null)));
    }

    @Test
    void soEnviaParaRevisaoOCursoCompletoEDepoisNaoEditaMais() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());

        CursoResponse vazio = cursosDaParceira.criar(conta, dadosDoCurso("Curso " + sufixo(), null));
        assertThrows(BadRequestException.class, () -> cursosDaParceira.enviarParaRevisao(conta, vazio.id()));   // sem módulos

        CursoResponse curso = cursoCompleto(conta);
        CursoResponse enviado = cursosDaParceira.enviarParaRevisao(conta, curso.id());
        assertEquals(SituacaoCurso.EM_REVISAO, enviado.situacao());
        assertFalse(enviado.ativo(), "em revisão ainda não aparece para as alunas");

        // em revisão ela espera: nada de editar nem de reenviar
        Long modulo = enviado.modulos().get(0).id();
        assertThrows(ConflictException.class, () -> cursosDaParceira.atualizar(conta, curso.id(), dadosDoCurso("Novo título", null)));
        assertThrows(ConflictException.class, () -> cursosDaParceira.criarModulo(conta, curso.id(), new ModuloGestaoRequest("X")));
        assertThrows(ConflictException.class, () -> cursosDaParceira.criarAula(conta, modulo, new AulaGestaoRequest("Y", 1, null, VIDEO)));
        assertThrows(ConflictException.class, () -> cursosDaParceira.enviarParaRevisao(conta, curso.id()));
    }

    @Test
    void oCursoEmRevisaoNaoApareceParaAsAlunas() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursoCompleto(conta);
        cursosDaParceira.enviarParaRevisao(conta, curso.id());

        Usuario aluna = new Usuario();
        aluna.setNome("Aluna");
        aluna.setEmail("al" + sufixo() + "@teste.com");
        aluna.setSenhaHash("x");
        Usuario salva = usuarios.save(aluna);

        assertFalse(cursos.findAllByAtivoTrue().stream().anyMatch(c -> c.getId().equals(curso.id())));
        assertThrows(NotFoundException.class, () -> cursoService.buscarCursoParaAluna(curso.titulo(), salva));
        assertFalse(cursoService.listarCursosDTO(null, salva).stream().anyMatch(c -> c.id().equals(curso.id())));
    }

    // ---------------------------------------------------------------- decisão da administradora

    @Test
    void aAdministradoraDevolveComComentarioEAParceiraAjustaEReenvia() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursoCompleto(conta);
        cursosDaParceira.enviarParaRevisao(conta, curso.id());

        assertThrows(BadRequestException.class, () -> gestao.mudarSituacao(curso.id(), SituacaoCurso.RASCUNHO),
                "sair da revisão sem comentário não pode");

        CursoResponse devolvido = gestao.devolver(curso.id(), "  A aula 1 está sem descrição.  ");
        assertEquals(SituacaoCurso.RASCUNHO, devolvido.situacao());
        assertEquals("A aula 1 está sem descrição.", devolvido.comentarioRevisao());
        // a parceira vê o comentário no próprio curso
        assertEquals("A aula 1 está sem descrição.", cursosDaParceira.detalhar(conta, curso.id()).comentarioRevisao());

        // pode editar de novo e reenviar
        cursosDaParceira.atualizar(conta, curso.id(), dadosDoCurso(curso.titulo(), null));
        assertEquals(SituacaoCurso.EM_REVISAO, cursosDaParceira.enviarParaRevisao(conta, curso.id()).situacao());

        // e devolver exige estar em revisão
        gestao.devolver(curso.id(), "Mais um ajuste.");
        assertThrows(ConflictException.class, () -> gestao.devolver(curso.id(), "De novo?"));
    }

    @Test
    void aprovarPublicaOCursoELimpaOComentario() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursoCompleto(conta);

        cursosDaParceira.enviarParaRevisao(conta, curso.id());
        gestao.devolver(curso.id(), "Ajuste a descrição.");
        cursosDaParceira.enviarParaRevisao(conta, curso.id());

        CursoResponse publicado = gestao.mudarSituacao(curso.id(), SituacaoCurso.PUBLICADO);

        assertEquals(SituacaoCurso.PUBLICADO, publicado.situacao());
        assertTrue(publicado.ativo());
        assertNull(publicado.comentarioRevisao());
        assertTrue(cursos.findAllByAtivoTrue().stream().anyMatch(c -> c.getId().equals(curso.id())));
    }

    @Test
    void recusarArquivaOCursoComOMotivo() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursoCompleto(conta);
        cursosDaParceira.enviarParaRevisao(conta, curso.id());

        CursoResponse recusado = gestao.recusar(curso.id(), "Conteúdo fora da proposta do app.");

        assertEquals(SituacaoCurso.ARQUIVADO, recusado.situacao());
        assertEquals("Conteúdo fora da proposta do app.", recusado.comentarioRevisao());
        assertThrows(ConflictException.class, () -> cursosDaParceira.atualizar(conta, curso.id(), dadosDoCurso("Outro", null)));
        assertThrows(ConflictException.class, () -> cursosDaParceira.enviarParaRevisao(conta, curso.id()));
    }

    @Test
    void aAdministradoraNaoMandaCursoParaRevisaoPeloPatch() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursoCompleto(conta);

        assertThrows(BadRequestException.class, () -> gestao.mudarSituacao(curso.id(), SituacaoCurso.EM_REVISAO));
    }

    @Test
    void cursoDoTechMentorNaoPassaPorRevisao() {
        CursoResponse proprio = gestao.criar(dadosDoCurso("Curso " + sufixo(), "Curso em Vídeo"));
        assertThrows(BadRequestException.class, () -> gestao.enviarParaRevisao(proprio.id()));
    }

    @Test
    void cursoPublicadoNaoFicaSemAulasNemSemModulos() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursoCompleto(conta);
        cursosDaParceira.enviarParaRevisao(conta, curso.id());
        gestao.mudarSituacao(curso.id(), SituacaoCurso.PUBLICADO);

        Long modulo = gestao.detalhar(curso.id()).modulos().get(0).id();
        Long aula = gestao.detalhar(curso.id()).modulos().get(0).aulas().get(0).id();

        // depois de publicado ela edita livremente, mas não pode deixar o módulo vazio nem o curso sem módulos
        assertThrows(ConflictException.class, () -> cursosDaParceira.apagarAula(conta, aula));
        assertThrows(ConflictException.class, () -> cursosDaParceira.apagarModulo(conta, modulo));

        cursosDaParceira.criarAula(conta, modulo, new AulaGestaoRequest("Aula 2", 5, null, VIDEO));
        cursosDaParceira.apagarAula(conta, aula);   // agora o módulo ainda tem uma aula
        assertEquals(1, gestao.detalhar(curso.id()).modulos().get(0).aulas().size());
    }

    // ---------------------------------------------------------------- avisos

    @Test
    void asNotificacoesLevamParaATelaCerta() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursoCompleto(conta);

        cursosDaParceira.enviarParaRevisao(conta, curso.id());

        Notificacao paraAdmin = aviso(admin, DestinoNotificacao.REVISAO_DE_CURSO, String.valueOf(curso.id()));
        assertEquals(TipoNotificacao.REVISAO, paraAdmin.getTipo());

        gestao.devolver(curso.id(), "Falta a descrição das aulas.");

        Notificacao paraParceira = aviso(conta, DestinoNotificacao.CURSO_DA_PARCEIRA, String.valueOf(curso.id()));
        assertEquals(TipoNotificacao.PARCERIA, paraParceira.getTipo());
        assertTrue(paraParceira.getMensagem().contains("Falta a descrição das aulas."), paraParceira.getMensagem());
    }

    @Test
    void aVerificacaoEOAceiteGeramAvisos() {
        Usuario admin = administradora();
        String email = email();
        ParceiraResponse criada = convidar(admin, "Instituto " + sufixo(), email);
        aceitar(email, ultimoCodigo());

        assertEquals(TipoNotificacao.PARCERIA,
                aviso(admin, DestinoNotificacao.PARCEIRA_PARA_VERIFICAR, String.valueOf(criada.id())).getTipo());

        parceiras.verificar(admin, criada.id(), checklistCompleto());
        Usuario conta = usuarios.findByEmail(email).orElseThrow();
        assertEquals("Sua conta foi verificada", aviso(conta, DestinoNotificacao.PERFIL_DA_PARCEIRA, null).getTitulo());
    }

    @Test
    void notificacaoAntigaSemTipoContaComoGeral() {
        Notificacao antiga = new Notificacao();
        assertEquals(TipoNotificacao.GERAL, antiga.getTipo());
        assertEquals(DestinoNotificacao.NENHUM, antiga.getDestino());
    }

    // ---------------------------------------------------------------- auditoria

    @Test
    void ohistoricoDoCursoGuardaQuemFezOQue() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());

        logarComo(conta);
        CursoResponse curso = cursoCompleto(conta);
        cursosDaParceira.enviarParaRevisao(conta, curso.id());

        logarComo(admin);
        gestao.devolver(curso.id(), "Ajuste.");

        List<RegistroResponse> historico = cursosDaParceira.historico(conta, curso.id());

        // do mais recente para o mais antigo
        assertEquals(AcaoAuditada.CURSO_DEVOLVIDO, historico.get(0).acao());
        assertEquals(admin.getNome(), historico.get(0).autorNome());
        assertEquals("ADMIN", historico.get(0).autorPapel());
        assertEquals("Ajuste.", historico.get(0).detalhe());

        assertEquals(AcaoAuditada.CURSO_ENVIADO_PARA_REVISAO, historico.get(1).acao());
        assertEquals(conta.getNome(), historico.get(1).autorNome());
        assertEquals("PARCEIRA", historico.get(1).autorPapel());

        List<AcaoAuditada> acoes = historico.stream().map(RegistroResponse::acao).toList();
        assertTrue(acoes.containsAll(List.of(AcaoAuditada.CURSO_CRIADO, AcaoAuditada.MODULO_CRIADO, AcaoAuditada.AULA_CRIADA)), acoes.toString());
    }

    @Test
    void ohistoricoDaParceiraRegistraConviteAceiteVerificacaoESuspensao() {
        Usuario admin = administradora();
        String email = email();
        ParceiraResponse criada = convidar(admin, "Instituto " + sufixo(), email);
        aceitar(email, ultimoCodigo());
        parceiras.verificar(admin, criada.id(), checklistCompleto());
        parceiras.suspender(admin, criada.id(), "Motivo de teste");
        parceiras.reativar(admin, criada.id());

        List<AcaoAuditada> acoes = auditoria.historico(RegistroAuditoria.PARCEIRA, criada.id()).stream()
                .map(RegistroResponse::acao).toList();

        assertEquals(List.of(AcaoAuditada.PARCERIA_REATIVADA, AcaoAuditada.PARCERIA_SUSPENSA, AcaoAuditada.PARCERIA_VERIFICADA,
                AcaoAuditada.PARCERIA_ACEITOU_TERMOS, AcaoAuditada.PARCERIA_CONVIDADA), acoes);

        // quem aceitou o convite aparece como autor do aceite (mesmo sem token)
        RegistroResponse aceite = auditoria.historico(RegistroAuditoria.PARCEIRA, criada.id()).get(3);
        assertEquals("PARCEIRA", aceite.autorPapel());
    }

    @Test
    void editarCursoSoRegistraQuandoAlgoMudou() {
        Usuario admin = administradora();
        Usuario conta = parceiraAtiva(admin, "Instituto " + sufixo());
        CursoResponse curso = cursosDaParceira.criar(conta, dadosDoCurso("Curso " + sufixo(), null));

        // sem mudança nenhuma: nada novo no histórico
        int antes = cursosDaParceira.historico(conta, curso.id()).size();
        cursosDaParceira.atualizar(conta, curso.id(), dadosDoCurso(curso.titulo(), null));
        assertEquals(antes, cursosDaParceira.historico(conta, curso.id()).size());

        cursosDaParceira.atualizar(conta, curso.id(), new CursoRequest(curso.titulo(), "Descrição nova.", Curso.Categoria.PROGRAMACAO,
                null, null, Curso.Nivel.INICIANTE, null, null));
        RegistroResponse ultimo = cursosDaParceira.historico(conta, curso.id()).get(0);
        assertEquals(AcaoAuditada.CURSO_EDITADO, ultimo.acao());
        assertEquals("Alterado: descrição.", ultimo.detalhe());
    }
}
