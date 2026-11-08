package com.techmentor.api;

import com.techmentor.api.dto.OportunidadeDtos.CandidaturaAdminResponse;
import com.techmentor.api.dto.OportunidadeDtos.InscricaoResponse;
import com.techmentor.api.dto.OportunidadeDtos.OportunidadeRequest;
import com.techmentor.api.dto.OportunidadeDtos.OportunidadeResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.NotificacaoService;
import com.techmentor.api.service.OportunidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Candidatura: a admin acompanha e muda a situação, a aluna é avisada e pode desistir; busca na lista. */
@SpringBootTest
@Transactional
class OportunidadesCandidaturaTest {

    @Autowired
    private OportunidadeService oportunidades;

    @Autowired
    private NotificacaoService notificacoes;

    @Autowired
    private UsuarioRepository usuarios;

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

    private OportunidadeResponse criar(Oportunidade.Tipo tipo, String titulo, String organizacao) {
        return oportunidades.criar(admin, new OportunidadeRequest(tipo, titulo, organizacao, "Descrição de teste",
                null, null, null, LocalDate.now().plusDays(10), null, "Programação", "Iniciante"));
    }

    private static HttpStatus statusDe(Executable acao) {
        return assertThrows(ApiException.class, acao).getStatus();
    }

    private long avisos(Usuario pessoa, String titulo) {
        return notificacoes.listarDTO(pessoa.getId(), null).stream()
                .filter(n -> titulo.equals(n.titulo()))
                .count();
    }

    @BeforeEach
    void preparar() {
        admin = criarUsuario(Usuario.Tipo.ADMIN);
        aluna = criarUsuario(Usuario.Tipo.ALUNA);
        outraAluna = criarUsuario(Usuario.Tipo.ALUNA);
        vaga = criar(Oportunidade.Tipo.EMPREGO, "Vaga Teste Unica Zeta", "Empresa Zeta");
        evento = criar(Oportunidade.Tipo.EVENTO, "Evento Teste Unico Zeta", "Comunidade Zeta");
    }

    @Test
    void adminMudaASituacaoEAAlunaEhAvisada() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id());
        assertEquals(InscricaoOportunidade.Status.ENVIADA, candidatura.status());

        CandidaturaAdminResponse nova = oportunidades.atualizarStatus(admin, candidatura.id(), InscricaoOportunidade.Status.APROVADA);

        assertEquals(InscricaoOportunidade.Status.APROVADA, nova.status());
        assertEquals(aluna.getId(), nova.usuarioId());
        assertEquals(1, avisos(aluna, "Atualização da sua candidatura"));
    }

    @Test
    void repetirAMesmaSituacaoNaoAvisaDeNovo() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id());

        oportunidades.atualizarStatus(admin, candidatura.id(), InscricaoOportunidade.Status.EM_ANALISE);
        oportunidades.atualizarStatus(admin, candidatura.id(), InscricaoOportunidade.Status.EM_ANALISE);

        assertEquals(1, avisos(aluna, "Atualização da sua candidatura"));
    }

    @Test
    void inscricaoEmEventoUsaOTextoDeInscricao() {
        InscricaoResponse inscricao = oportunidades.inscreverEmEvento(aluna, evento.id());

        oportunidades.atualizarStatus(admin, inscricao.id(), InscricaoOportunidade.Status.APROVADA);

        assertEquals(1, avisos(aluna, "Atualização da sua inscrição"));
    }

    @Test
    void soAdminVeAsCandidaturasEMudaASituacao() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id());

        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> oportunidades.candidaturasDaOportunidade(aluna, vaga.id())));
        assertEquals(HttpStatus.FORBIDDEN, statusDe(() ->
                oportunidades.atualizarStatus(aluna, candidatura.id(), InscricaoOportunidade.Status.APROVADA)));

        List<CandidaturaAdminResponse> lista = oportunidades.candidaturasDaOportunidade(admin, vaga.id());
        assertEquals(1, lista.size());
        assertEquals(aluna.getNome(), lista.get(0).usuarioNome());
        assertEquals(aluna.getEmail(), lista.get(0).usuarioEmail());
    }

    @Test
    void candidaturaInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, statusDe(() ->
                oportunidades.atualizarStatus(admin, 999_999L, InscricaoOportunidade.Status.APROVADA)));
        assertEquals(HttpStatus.NOT_FOUND, statusDe(() -> oportunidades.desistir(aluna, 999_999L)));
    }

    @Test
    void aAlunaDesisteEntaoPodeSeCandidatarDeNovo() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id());

        oportunidades.desistir(aluna, candidatura.id());

        assertTrue(oportunidades.minhasInscricoes(aluna, false).isEmpty());
        assertEquals(InscricaoOportunidade.Status.ENVIADA, oportunidades.candidatar(aluna, vaga.id()).status());
    }

    @Test
    void soADonaDesisteEDepoisDaRespostaNaoDa() {
        InscricaoResponse candidatura = oportunidades.candidatar(aluna, vaga.id());

        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> oportunidades.desistir(outraAluna, candidatura.id())));

        oportunidades.atualizarStatus(admin, candidatura.id(), InscricaoOportunidade.Status.RECUSADA);
        assertEquals(HttpStatus.CONFLICT, statusDe(() -> oportunidades.desistir(aluna, candidatura.id())));
    }

    @Test
    void buscaProcuraNoTituloNaOrganizacaoENaDescricao() {
        List<String> porTitulo = oportunidades.listar(null, null, "teste unica zeta").stream()
                .map(OportunidadeResponse::titulo).toList();
        assertEquals(List.of("Vaga Teste Unica Zeta"), porTitulo);

        List<String> porOrganizacao = oportunidades.listar(null, null, "COMUNIDADE ZETA").stream()
                .map(OportunidadeResponse::titulo).toList();
        assertEquals(List.of("Evento Teste Unico Zeta"), porOrganizacao);

        assertFalse(oportunidades.listar(null, null, "   ").isEmpty());
        assertTrue(oportunidades.listar(null, null, "nada-disso-existe-xyz").isEmpty());
    }

    @Test
    void buscaSeCombinaComOFiltroDeTipo() {
        List<String> soEventos = oportunidades.listar(Oportunidade.Tipo.EVENTO, null, "zeta").stream()
                .map(OportunidadeResponse::titulo).toList();
        assertEquals(List.of("Evento Teste Unico Zeta"), soEventos);
    }
}
