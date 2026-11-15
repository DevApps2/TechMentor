package com.techmentor.api;

import com.techmentor.api.dto.CursoDtos.AulaResponse;
import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import com.techmentor.api.dto.GestaoCursoDtos.AulaGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.MetricasCursoResponse;
import com.techmentor.api.dto.GestaoCursoDtos.ModuloGestaoRequest;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Aula;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Matricula;
import com.techmentor.api.model.ProgressoAula;
import com.techmentor.api.model.SituacaoCurso;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.AulaRepository;
import com.techmentor.api.repository.CursoRepository;
import com.techmentor.api.repository.MatriculaRepository;
import com.techmentor.api.repository.ProgressoAulaRepository;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.CursoService;
import com.techmentor.api.service.GestaoCursoService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As regras da gestão de cursos: rascunho e publicação, proteção de quem já estudou, reordenar e métricas. */
@SpringBootTest
@Transactional
class GestaoCursoTest {

    private static final String VIDEO = "https://www.youtube.com/watch?v=sTX0UEplF54";
    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Autowired private GestaoCursoService gestao;
    @Autowired private CursoService cursoService;
    @Autowired private CursoRepository cursos;
    @Autowired private AulaRepository aulas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private ProgressoAulaRepository progressos;

    // ---------------------------------------------------------------- apoio

    private static String titulo() {
        return "Curso " + UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario aluna() {
        Usuario u = new Usuario();
        u.setNome("Aluna Teste");
        u.setEmail("a" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(Usuario.Tipo.ALUNA);
        return usuarios.save(u);
    }

    private CursoResponse criarCurso(String titulo) {
        return gestao.criar(new CursoRequest(titulo, "Descrição do curso.", Curso.Categoria.PROGRAMACAO, null, null,
                Curso.Nivel.INICIANTE, "Curso em Vídeo", null));
    }

    private AulaGestaoRequest aulaNova(String titulo) {
        return new AulaGestaoRequest(titulo, 10, null, VIDEO);
    }

    /** Um curso rascunho com um módulo e três aulas (A, B, C), ainda sem publicar. */
    private Curso cursoComTresAulas() {
        CursoResponse curso = criarCurso(titulo());
        ModuloResponse modulo = gestao.criarModulo(curso.id(), new ModuloGestaoRequest("Módulo 1"));
        gestao.criarAula(modulo.id(), aulaNova("Aula A"));
        gestao.criarAula(modulo.id(), aulaNova("Aula B"));
        gestao.criarAula(modulo.id(), aulaNova("Aula C"));
        return cursos.findById(curso.id()).orElseThrow();
    }

    private void concluir(Usuario aluna, Aula aula) {
        ProgressoAula progresso = new ProgressoAula();
        progresso.setUsuario(aluna);
        progresso.setAula(aula);
        progressos.save(progresso);
    }

    private Matricula matricular(Usuario aluna, Curso curso) {
        Matricula matricula = new Matricula();
        matricula.setUsuario(aluna);
        matricula.setCurso(curso);
        return matriculas.save(matricula);
    }

    private static List<String> titulosDasAulas(ModuloResponse modulo) {
        return modulo.aulas().stream().map(AulaResponse::titulo).toList();
    }

    // ---------------------------------------------------------------- rascunho e publicação

    @Test
    void cursoNovoNasceEmRascunhoEFicaForaDoCatalogoDaAluna() {
        CursoResponse curso = criarCurso(titulo());

        assertEquals(SituacaoCurso.RASCUNHO, curso.situacao());
        assertFalse(curso.ativo());
        assertEquals("Curso em Vídeo", curso.instituicao());

        boolean apareceParaAluna = cursoService.listarCursosDTO(null, aluna()).stream()
                .anyMatch(c -> c.id().equals(curso.id()));
        assertFalse(apareceParaAluna);

        boolean apareceParaAdmin = gestao.listar(SituacaoCurso.RASCUNHO).stream()
                .anyMatch(c -> c.id().equals(curso.id()));
        assertTrue(apareceParaAdmin);
    }

    @Test
    void soPublicaCursoCompleto() {
        CursoResponse curso = criarCurso(titulo());

        // sem módulo
        assertThrows(BadRequestException.class, () -> gestao.mudarSituacao(curso.id(), SituacaoCurso.PUBLICADO));

        // módulo sem aula
        ModuloResponse modulo = gestao.criarModulo(curso.id(), new ModuloGestaoRequest("Módulo 1"));
        assertThrows(BadRequestException.class, () -> gestao.mudarSituacao(curso.id(), SituacaoCurso.PUBLICADO));

        // completo
        gestao.criarAula(modulo.id(), aulaNova("Aula A"));
        CursoResponse publicado = gestao.mudarSituacao(curso.id(), SituacaoCurso.PUBLICADO);

        assertEquals(SituacaoCurso.PUBLICADO, publicado.situacao());
        assertTrue(publicado.ativo());
        assertTrue(cursoService.listarCursosDTO(null, aluna()).stream().anyMatch(c -> c.id().equals(curso.id())));
    }

    @Test
    void alunaMatriculadaContinuaComAcessoAoCursoArquivado() {
        Curso curso = cursoComTresAulas();
        gestao.mudarSituacao(curso.getId(), SituacaoCurso.PUBLICADO);

        Usuario estudando = aluna();
        Usuario outra = aluna();
        matricular(estudando, curso);

        gestao.mudarSituacao(curso.getId(), SituacaoCurso.ARQUIVADO);

        // quem já estava matriculada continua vendo e estudando
        assertTrue(cursoService.listarCursosDTO(null, estudando).stream().anyMatch(c -> c.id().equals(curso.getId())));
        assertEquals(curso.getId(), cursoService.buscarCursoParaAluna(curso.getTitulo(), estudando).getId());

        // quem não estava não vê, e não consegue abrir
        assertFalse(cursoService.listarCursosDTO(null, outra).stream().anyMatch(c -> c.id().equals(curso.getId())));
        assertThrows(NotFoundException.class, () -> cursoService.buscarCursoParaAluna(curso.getTitulo(), outra));
    }

    @Test
    void naoVoltaParaRascunhoComAlunasMatriculadas() {
        Curso curso = cursoComTresAulas();
        gestao.mudarSituacao(curso.getId(), SituacaoCurso.PUBLICADO);
        matricular(aluna(), curso);

        assertThrows(ConflictException.class, () -> gestao.mudarSituacao(curso.getId(), SituacaoCurso.RASCUNHO));
    }

    // ---------------------------------------------------------------- quem já estudou fica protegida

    @Test
    void aulaConcluidaPorAlunaNaoSeApaga() {
        Curso curso = cursoComTresAulas();
        Aula primeira = curso.getModulos().get(0).getAulas().get(0);
        concluir(aluna(), primeira);

        ConflictException erro = assertThrows(ConflictException.class, () -> gestao.apagarAula(primeira.getId()));

        assertTrue(erro.getMessage().contains("1 aluna já estudou"), erro.getMessage());
        assertTrue(aulas.existsById(primeira.getId()));
    }

    @Test
    void aulaSemProgressoSeApagaEAsOutrasFechamOBuraco() {
        Curso curso = cursoComTresAulas();
        Aula segunda = curso.getModulos().get(0).getAulas().get(1);

        gestao.apagarAula(segunda.getId());

        ModuloResponse modulo = gestao.detalhar(curso.getId()).modulos().get(0);
        assertEquals(List.of("Aula A", "Aula C"), titulosDasAulas(modulo));
        assertEquals(List.of(1, 2), modulo.aulas().stream().map(AulaResponse::ordem).toList());
    }

    @Test
    void moduloComProgressoNaoSeApaga() {
        Curso curso = cursoComTresAulas();
        concluir(aluna(), curso.getModulos().get(0).getAulas().get(0));

        assertThrows(ConflictException.class, () -> gestao.apagarModulo(curso.getModulos().get(0).getId()));
    }

    // ---------------------------------------------------------------- reordenar

    @Test
    void reordenaAsAulasDeUmModulo() {
        Curso curso = cursoComTresAulas();
        var modulo = curso.getModulos().get(0);
        Long a = modulo.getAulas().get(0).getId();
        Long b = modulo.getAulas().get(1).getId();
        Long c = modulo.getAulas().get(2).getId();

        ModuloResponse resultado = gestao.reordenarAulas(modulo.getId(), List.of(c, a, b));

        assertEquals(List.of("Aula C", "Aula A", "Aula B"), titulosDasAulas(resultado));
        assertEquals(List.of(1, 2, 3), resultado.aulas().stream().map(AulaResponse::ordem).toList());
    }

    @Test
    void reordenarExigeExatamenteAsAulasDoModulo() {
        Curso curso = cursoComTresAulas();
        var modulo = curso.getModulos().get(0);
        Long a = modulo.getAulas().get(0).getId();
        Long b = modulo.getAulas().get(1).getId();

        assertThrows(BadRequestException.class, () -> gestao.reordenarAulas(modulo.getId(), List.of(a, b)));       // falta uma
        assertThrows(BadRequestException.class, () -> gestao.reordenarAulas(modulo.getId(), List.of(a, a, b)));    // repetida
        assertThrows(BadRequestException.class, () -> gestao.reordenarAulas(modulo.getId(), List.of(a, b, 999_999L))); // de fora
    }

    @Test
    void naoReordenaQuandoJaTemAlunaEstudando() {
        Curso curso = cursoComTresAulas();
        var modulo = curso.getModulos().get(0);
        concluir(aluna(), modulo.getAulas().get(0));
        List<Long> invertida = List.of(modulo.getAulas().get(2).getId(), modulo.getAulas().get(1).getId(),
                modulo.getAulas().get(0).getId());

        assertThrows(ConflictException.class, () -> gestao.reordenarAulas(modulo.getId(), invertida));
    }

    @Test
    void reordenaOsModulosDoCurso() {
        CursoResponse curso = criarCurso(titulo());
        ModuloResponse primeiro = gestao.criarModulo(curso.id(), new ModuloGestaoRequest("Primeiro"));
        ModuloResponse segundo = gestao.criarModulo(curso.id(), new ModuloGestaoRequest("Segundo"));

        CursoResponse resultado = gestao.reordenarModulos(curso.id(), List.of(segundo.id(), primeiro.id()));

        assertEquals(List.of("Segundo", "Primeiro"), resultado.modulos().stream().map(ModuloResponse::titulo).toList());
        assertEquals(List.of(1, 2), resultado.modulos().stream().map(ModuloResponse::ordem).toList());
    }

    // ---------------------------------------------------------------- métricas

    @Test
    void metricasContamMatriculasConcluintesEOndeAsAlunasPararam() {
        Curso curso = cursoComTresAulas();
        var aulasDoCurso = curso.getModulos().get(0).getAulas();

        Usuario terminou = aluna();
        Usuario parouNaPrimeira = aluna();
        Matricula m1 = matricular(terminou, curso);
        matricular(parouNaPrimeira, curso);

        aulasDoCurso.forEach(aula -> concluir(terminou, aula));
        m1.setConcluidoEm(LocalDateTime.now());
        matriculas.save(m1);
        concluir(parouNaPrimeira, aulasDoCurso.get(0));

        MetricasCursoResponse m = gestao.metricas(curso.getId());

        assertEquals(2, m.matriculas());
        assertEquals(1, m.concluintes());
        assertEquals(2, m.alunasComProgresso());
        assertEquals(50, m.taxaConclusao());
        assertEquals(List.of(2L, 1L, 1L), m.aulas().stream().map(a -> a.concluiram()).toList());
        assertNotNull(m.aulaOndeMaisPararam());
        assertEquals("Aula A", m.aulaOndeMaisPararam().titulo());   // uma aluna concluiu a A e parou
        assertEquals(1, m.alunasQuePararam());
    }

    @Test
    void metricasDeCursoSemMovimentoNaoInventamNada() {
        MetricasCursoResponse m = gestao.metricas(cursoComTresAulas().getId());

        assertEquals(0, m.matriculas());
        assertEquals(0, m.taxaConclusao());
        assertNull(m.aulaOndeMaisPararam());
    }

    // ---------------------------------------------------------------- link do vídeo

    @Test
    void linkDoVideoAceitaYoutubeEMp4SoEmHttps() {
        for (String ok : List.of("https://www.youtube.com/watch?v=sTX0UEplF54",
                "https://youtube.com/watch?v=sTX0UEplF54&t=30s",
                "https://youtu.be/sTX0UEplF54",
                "https://exemplo.com/aulas/aula-01.mp4")) {
            assertTrue(VALIDADOR.validate(new AulaGestaoRequest("Aula", 10, null, ok)).isEmpty(), "deveria aceitar: " + ok);
        }

        for (String ruim : List.of("https://qualquercoisa.com",
                "http://www.youtube.com/watch?v=sTX0UEplF54",   // sem https
                "https://www.youtube.com/watch?v=curto",         // id de tamanho errado
                "https://www.youtube.com/",                      // sem vídeo
                "video.mp4")) {
            assertFalse(VALIDADOR.validate(new AulaGestaoRequest("Aula", 10, null, ruim)).isEmpty(), "deveria recusar: " + ruim);
        }
    }
}
