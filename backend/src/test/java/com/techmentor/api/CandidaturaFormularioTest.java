package com.techmentor.api;

import com.techmentor.api.dto.OportunidadeDtos.CandidaturaAdminResponse;
import com.techmentor.api.dto.OportunidadeDtos.CandidaturaRequest;
import com.techmentor.api.dto.OportunidadeDtos.InscricaoResponse;
import com.techmentor.api.dto.OportunidadeDtos.OportunidadeRequest;
import com.techmentor.api.dto.OportunidadeDtos.OportunidadeResponse;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.OportunidadeService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O formulário de candidatura: os dados chegam à administradora, o PDF é conferido e só ela o baixa. */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.pasta-curriculos=target/curriculos-de-teste")
class CandidaturaFormularioTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired private OportunidadeService oportunidades;
    @Autowired private UsuarioRepository usuarios;

    private Usuario admin;
    private Usuario aluna;
    private Usuario outraAluna;
    private OportunidadeResponse vaga;
    private OportunidadeResponse evento;

    private Usuario criarUsuario(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail("t" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private OportunidadeResponse criar(Oportunidade.Tipo tipo, String titulo) {
        return oportunidades.criar(admin, new OportunidadeRequest(tipo, titulo, "Empresa", "Descrição de teste",
                null, null, null, LocalDate.now().plusDays(10), null, "Programação", "Iniciante"));
    }

    private static CandidaturaRequest formulario() {
        return new CandidaturaRequest("  Camila Rocha  ", " Camila@Email.com ", "(11) 99999-9999",
                " São Paulo, SP ", "https://linkedin.com/in/camila", "Quero muito aprender.");
    }

    private static MockMultipartFile pdf(byte[] conteudo, String nome) {
        return new MockMultipartFile("curriculo", nome, "application/pdf", conteudo);
    }

    private CandidaturaAdminResponse comoAdminVe(InscricaoResponse candidatura) {
        return oportunidades.candidaturasDaOportunidade(admin, candidatura.oportunidadeId()).stream()
                .filter(c -> c.id().equals(candidatura.id()))
                .findFirst().orElseThrow();
    }

    @BeforeEach
    void preparar() {
        admin = criarUsuario(Usuario.Tipo.ADMIN);
        aluna = criarUsuario(Usuario.Tipo.ALUNA);
        outraAluna = criarUsuario(Usuario.Tipo.ALUNA);
        vaga = criar(Oportunidade.Tipo.EMPREGO, "Vaga Formulario " + UUID.randomUUID().toString().substring(0, 6));
        evento = criar(Oportunidade.Tipo.EVENTO, "Evento Formulario " + UUID.randomUUID().toString().substring(0, 6));
    }

    // ---------------------------------------------------------------- dados do formulário

    @Test
    void osDadosDoFormularioChegamAAdministradora() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        CandidaturaAdminResponse vista = comoAdminVe(candidatura);

        assertEquals("Camila Rocha", vista.contatoNome());          // sem os espaços das pontas
        assertEquals("camila@email.com", vista.contatoEmail());     // e-mail em minúsculas
        assertEquals("(11) 99999-9999", vista.telefone());
        assertEquals("São Paulo, SP", vista.cidade());
        assertEquals("https://linkedin.com/in/camila", vista.linkProfissional());
        assertEquals("Quero muito aprender.", vista.mensagem());
        assertFalse(vista.temCurriculo());
    }

    @Test
    void semCorpoACandidaturaContinuaFuncionando() {
        // o app antigo e as inscrições em evento não mandam dados
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id());
        CandidaturaAdminResponse vista = comoAdminVe(candidatura);

        assertNull(vista.contatoNome());
        assertNull(vista.telefone());
        assertFalse(vista.temCurriculo());

        InscricaoResponse inscricao = oportunidades.inscreverEmEvento(aluna, evento.id());
        assertEquals(InscricaoOportunidade.Status.ENVIADA, inscricao.status());
    }

    @Test
    void camposOpcionaisEmBrancoViramNulos() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(),
                new CandidaturaRequest("Camila Rocha", "camila@email.com", "11999999999", "   ", "", " "));

        CandidaturaAdminResponse vista = comoAdminVe(candidatura);

        assertNull(vista.cidade());
        assertNull(vista.linkProfissional());
        assertNull(vista.mensagem());
    }

    @Test
    void oFormularioRecusaDadosInvalidos() {
        assertTrue(VALIDADOR.validate(formulario()).isEmpty());

        assertFalse(VALIDADOR.validate(new CandidaturaRequest("Ca", "camila@email.com", "11999999999", null, null, null)).isEmpty());
        assertFalse(VALIDADOR.validate(new CandidaturaRequest("Camila Rocha", "isso-nao-e-email", "11999999999", null, null, null)).isEmpty());
        assertFalse(VALIDADOR.validate(new CandidaturaRequest("Camila Rocha", "camila@email.com", "12345", null, null, null)).isEmpty());
        assertFalse(VALIDADOR.validate(new CandidaturaRequest("Camila Rocha", "camila@email.com", "11999999999", null, "javascript:alert(1)", null)).isEmpty());
        assertFalse(VALIDADOR.validate(new CandidaturaRequest("Camila Rocha", "camila@email.com", "11999999999", null, null, "x".repeat(501))).isEmpty());
    }

    // ---------------------------------------------------------------- currículo em PDF

    @Test
    void oPdfFicaGuardadoEAAdministradoraBaixa() throws Exception {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(PDF, "Meu Curriculo.pdf"));

        CandidaturaAdminResponse vista = comoAdminVe(candidatura);
        assertTrue(vista.temCurriculo());
        assertEquals("Meu Curriculo.pdf", vista.nomeDoCurriculo());

        var guardado = oportunidades.curriculoDaCandidatura(admin, candidatura.id());
        assertEquals("Meu Curriculo.pdf", guardado.nome());
        assertEquals(PDF.length, Files.readAllBytes(guardado.caminho()).length);
    }

    @Test
    void arquivoQueNaoEPdfEhRecusadoMesmoComTipoFalso() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        byte[] naoEPdf = "<html>isso nao e um pdf</html>".getBytes(StandardCharsets.UTF_8);
        assertThrows(BadRequestException.class, () -> oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(naoEPdf, "falso.pdf")));
        assertThrows(BadRequestException.class, () -> oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(new byte[0], "vazio.pdf")));

        assertFalse(comoAdminVe(candidatura).temCurriculo());
    }

    @Test
    void pdfMaiorQueCincoMegasEhRecusado() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, grande, 0, 8);

        assertThrows(BadRequestException.class, () -> oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(grande, "grande.pdf")));
    }

    @Test
    void soADonaAnexaEODownloadEDaAdministradora() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        assertThrows(ForbiddenException.class, () -> oportunidades.anexarCurriculo(outraAluna, candidatura.id(), pdf(PDF, "x.pdf")));

        oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(PDF, "meu.pdf"));

        // nem a dona nem outra aluna baixam: só a administradora
        assertThrows(ForbiddenException.class, () -> oportunidades.curriculoDaCandidatura(aluna, candidatura.id()));
        assertThrows(ForbiddenException.class, () -> oportunidades.curriculoDaCandidatura(outraAluna, candidatura.id()));
    }

    @Test
    void semCurriculoODownloadDaNaoEncontrado() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        assertThrows(NotFoundException.class, () -> oportunidades.curriculoDaCandidatura(admin, candidatura.id()));
    }

    @Test
    void trocarOPdfApagaOAnterior() throws Exception {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(PDF, "primeiro.pdf"));
        var primeiro = oportunidades.curriculoDaCandidatura(admin, candidatura.id());

        oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(PDF, "segundo.pdf"));
        var segundo = oportunidades.curriculoDaCandidatura(admin, candidatura.id());

        assertEquals("segundo.pdf", segundo.nome());
        assertFalse(Files.exists(primeiro.caminho()), "o PDF antigo não pode ficar guardado");
        assertTrue(Files.exists(segundo.caminho()));
    }

    @Test
    void desistirApagaOPdfGuardado() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());
        oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(PDF, "meu.pdf"));
        var guardado = oportunidades.curriculoDaCandidatura(admin, candidatura.id());

        oportunidades.desistir(aluna, candidatura.id());

        assertFalse(Files.exists(guardado.caminho()), "quem desiste leva o currículo embora");
    }

    @Test
    void depoisDaRespostaNaoSeAnexaMais() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());
        oportunidades.atualizarStatus(admin, candidatura.id(), InscricaoOportunidade.Status.RECUSADA);

        assertThrows(ConflictException.class, () -> oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(PDF, "tarde.pdf")));
    }

    @Test
    void nomeDoArquivoNaoCarregaPastas() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id(), formulario());

        oportunidades.anexarCurriculo(aluna, candidatura.id(), pdf(PDF, "../../etc/passwd.pdf"));

        assertEquals("passwd.pdf", comoAdminVe(candidatura).nomeDoCurriculo());
        assertTrue(oportunidades.curriculoDaCandidatura(admin, candidatura.id()).caminho().toString().contains("curriculos-de-teste"));
    }
}
