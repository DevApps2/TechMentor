package com.techmentor.api;

import com.techmentor.api.model.Parceira;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.TipoParceira;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.ParceiraRepository;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A matriz de acesso: para cada papel (sem login, aluna, mentora, administradora, parceira), o que cada rota responde.
 * Roda por HTTP de verdade, com a SecurityConfig inteira. Serve de rede de proteção: se alguém abrir uma rota
 * sem querer (ou fechar uma que devia estar aberta), um destes testes quebra.
 * Respostas esperadas: 401 sem login, 403 com login mas sem permissão.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ParceiraAcessoTest {

    private static final String SEM_LOGIN = "";

    @Value("${local.server.port}")
    private int porta;

    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ParceiraRepository parceiras;

    private final HttpClient http = HttpClient.newHttpClient();

    private Usuario aluna;
    private Usuario mentora;
    private Usuario admin;
    private Usuario parceira;
    private Usuario outraParceira;
    private Usuario parceiraSuspensa;

    private record Resp(int status, String corpo) { }

    @BeforeEach
    void criarPersonagens() {
        aluna = conta(Usuario.Tipo.ALUNA);
        mentora = conta(Usuario.Tipo.MENTORA);
        admin = conta(Usuario.Tipo.ADMIN);
        parceira = contaDeParceira(SituacaoParceira.ATIVA);
        outraParceira = contaDeParceira(SituacaoParceira.ATIVA);
        parceiraSuspensa = contaDeParceira(SituacaoParceira.SUSPENSA);
    }

    // ---------------------------------------------------------------- apoio

    private Usuario conta(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail(tipo.name().toLowerCase() + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private Usuario contaDeParceira(SituacaoParceira situacao) {
        Usuario conta = conta(Usuario.Tipo.PARCEIRA);
        Parceira p = new Parceira();
        p.setUsuario(conta);
        p.setNome("Instituto " + UUID.randomUUID().toString().substring(0, 8));
        p.setTipo(TipoParceira.ONG);
        p.setResponsavel("Responsável");
        p.setSituacao(situacao);
        p.setTermosVersao("2026-11-14");
        p.setTermosAceitosEm(java.time.LocalDateTime.now());
        parceiras.save(p);
        return conta;
    }

    private Resp chamar(String metodo, String caminho, String json, Usuario quem) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json");
        if (quem != null) req.header("Authorization", "Bearer " + jwtService.gerarToken(String.valueOf(quem.getId())));
        req.method(metodo, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        HttpResponse<String> r = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Resp(r.statusCode(), r.body());
    }

    private void espera(int status, String metodo, String caminho, String json, Usuario quem) throws Exception {
        Resp r = chamar(metodo, caminho, json, quem);
        String papel = quem == null ? "sem login" : quem.getTipo().name();
        assertEquals(status, r.status(), metodo + " " + caminho + " como " + papel + " -> " + r.corpo());
    }

    private static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\"\\s*:\\s*(\"[^\"]*\"|[0-9.]+|true|false|null)").matcher(json);
        assertTrue(m.find(), "campo '" + nome + "' não encontrado em: " + json);
        return m.group(1).replace("\"", "");
    }

    private static String cursoJson(String titulo) {
        return "{\"titulo\":\"" + titulo + "\",\"descricao\":\"Descrição.\",\"categoria\":\"PROGRAMACAO\"}";
    }

    // ---------------------------------------------------------------- rotas abertas

    @Test
    void osTermosEOAceiteDoConviteSaoPublicos() throws Exception {
        espera(200, "GET", "/parceiras/termos", null, null);

        // sem token chega até a validação (400/…), nunca no 401
        Resp aceite = chamar("POST", "/parceiras/convite/aceitar", "{}", null);
        assertEquals(400, aceite.status(), aceite.corpo());
    }

    @Test
    void ostermosTrazAVersaoETexto() throws Exception {
        Resp r = chamar("GET", "/parceiras/termos", null, null);
        assertEquals(200, r.status());
        assertFalse(campo(r.corpo(), "versao").isBlank());
        assertTrue(r.corpo().contains("Termos de Parceria"));
    }

    // ---------------------------------------------------------------- /api/parceira: só o papel PARCEIRA

    @Test
    void aAreaDaParceiraEDeMaisNinguem() throws Exception {
        String[] rotas = {"/api/parceira/perfil", "/api/parceira/cursos"};

        for (String rota : rotas) {
            String url = rota;
            espera(401, "GET", url, null, null);
            espera(403, "GET", url, null, aluna);
            espera(403, "GET", url, null, mentora);
            espera(403, "GET", url, null, admin);
            espera(200, "GET", url, null, parceira);
        }

        // escrita também: ninguém além da parceira cria curso por aqui
        String criar = "/api/parceira/cursos";
        espera(401, "POST", criar, cursoJson("Curso X"), null);
        espera(403, "POST", criar, cursoJson("Curso X"), aluna);
        espera(403, "POST", criar, cursoJson("Curso X"), mentora);
        espera(403, "POST", criar, cursoJson("Curso X"), admin);
    }

    // ---------------------------------------------------------------- /api/admin: só a administradora

    @Test
    void agestaoDeParceirasECursosEDaAdministradora() throws Exception {
        String[] rotas = {"/api/admin/parceiras", "/api/admin/cursos"};

        for (String rota : rotas) {
            String url = rota;
            espera(401, "GET", url, null, null);
            espera(403, "GET", url, null, aluna);
            espera(403, "GET", url, null, mentora);
            espera(403, "GET", url, null, parceira);
            espera(403, "GET", url, null, parceiraSuspensa);
            espera(200, "GET", url, null, admin);
        }

        // convidar, verificar, suspender, devolver, recusar: nada disso é da parceira
        espera(403, "POST", "/api/admin/parceiras", "{}", parceira);
        espera(403, "PUT", "/api/admin/parceiras/1/verificacao", "{}", parceira);
        espera(403, "POST", "/api/admin/parceiras/1/suspensao", "{}", parceira);
        espera(403, "POST", "/api/admin/parceiras/1/reativacao", null, parceira);
        espera(403, "POST", "/api/admin/cursos/1/devolucao", "{}", parceira);
        espera(403, "POST", "/api/admin/cursos/1/recusa", "{}", parceira);
        espera(403, "PATCH", "/api/admin/cursos/1/situacao", "{\"situacao\":\"PUBLICADO\"}", parceira);
        espera(403, "POST", "/api/admin/parceiras", "{}", aluna);

        // a administradora chega até a validação do corpo
        espera(400, "POST", "/api/admin/parceiras", "{}", admin);
    }

    // ---------------------------------------------------------------- a parceira não entra no app das alunas

    @Test
    void oCatalogoDeCursosEDasAlunasEMentoras() throws Exception {
        espera(401, "GET", "/cursos", null, null);
        espera(200, "GET", "/cursos", null, aluna);
        espera(200, "GET", "/cursos", null, mentora);
        espera(403, "GET", "/cursos", null, parceira);
        espera(403, "GET", "/cursos/Algoritmos", null, parceira);
        espera(403, "GET", "/cursos/buscar?termo=java", null, parceira);
    }

    @Test
    void osDadosPessoaisDoAppDasAlunasFicamFechadosParaAParceira() throws Exception {
        String id = String.valueOf(parceira.getId());
        String idDaAluna = String.valueOf(aluna.getId());

        // a aluna enxerga os dela (200) e a parceira, nem os próprios (403)
        espera(200, "GET", "/matriculas/usuarios/" + idDaAluna, null, aluna);
        espera(403, "GET", "/matriculas/usuarios/" + id, null, parceira);
        espera(403, "POST", "/matriculas/Algoritmos/usuarios/" + id, null, parceira);
        espera(403, "GET", "/conquistas/usuarios/" + id, null, parceira);
        espera(403, "GET", "/desafios/usuarios/" + id, null, parceira);
        espera(403, "GET", "/historico-xp/usuarios/" + id, null, parceira);

        // busca de pessoas (para denunciar ou bloquear) é do app das alunas
        espera(200, "GET", "/usuarios/busca?nome=ab", null, aluna);
        espera(403, "GET", "/usuarios/busca?nome=ab", null, parceira);

        // qualquer rota nova, sem regra própria, nasce fechada para a parceira
        espera(403, "GET", "/mentoras", null, parceira);
        espera(403, "GET", "/oportunidades", null, parceira);
        espera(403, "GET", "/mentorias", null, parceira);
    }

    @Test
    void aParceiraTemAPropriaContaENotificacoes() throws Exception {
        String id = String.valueOf(parceira.getId());
        String idDaOutra = String.valueOf(outraParceira.getId());

        espera(200, "GET", "/usuarios/" + id, null, parceira);
        espera(200, "GET", "/notificacoes/usuarios/" + id, null, parceira);
        espera(204, "PATCH", "/notificacoes/leitura/usuarios/" + id, null, parceira);

        // mas só as dela: a conta e as notificações de outra pessoa continuam fechadas
        espera(403, "GET", "/notificacoes/usuarios/" + idDaOutra, null, parceira);
        espera(403, "GET", "/notificacoes/usuarios/" + id, null, aluna);
        espera(403, "GET", "/notificacoes/usuarios/" + id, null, admin);   // nem a administradora lê as dos outros
        espera(403, "PUT", "/usuarios/" + idDaOutra, "{\"nome\":\"Invasora\",\"email\":\"i@i.com\"}", parceira);
    }

    // ---------------------------------------------------------------- alunas e mentoras seguem como antes

    @Test
    void alunaEMentoraContinuamForaDeRotasDeAdministradora() throws Exception {
        espera(403, "POST", "/cursos", cursoJson("Curso Y"), aluna);
        espera(403, "GET", "/usuarios", null, mentora);
        espera(403, "POST", "/api/admin/mentoras", "{}", aluna);
        espera(200, "GET", "/usuarios", null, admin);
    }

    // ---------------------------------------------------------------- uma parceira inteira, por HTTP

    @Test
    void aParceiraAtivaCriaUmCursoPorHttpEOutraNaoEnxergaOCurso() throws Exception {
        String titulo = "Curso " + UUID.randomUUID().toString().substring(0, 8);

        Resp criado = chamar("POST", "/api/parceira/cursos", cursoJson(titulo), parceira);
        assertEquals(201, criado.status(), criado.corpo());
        assertEquals("RASCUNHO", campo(criado.corpo(), "situacao"));
        String id = campo(criado.corpo(), "id");

        espera(200, "GET", "/api/parceira/cursos/" + id, null, parceira);
        espera(404, "GET", "/api/parceira/cursos/" + id, null, outraParceira);
        espera(404, "PUT", "/api/parceira/cursos/" + id, cursoJson("Outro título"), outraParceira);

        // a administradora enxerga (na fila dela), e a aluna não vê rascunho no catálogo
        espera(200, "GET", "/api/admin/cursos/" + id, null, admin);
        Resp catalogo = chamar("GET", "/cursos", null, aluna);
        assertEquals(200, catalogo.status());
        assertFalse(catalogo.corpo().contains(titulo), "rascunho não pode aparecer no catálogo das alunas");

        // enviar sem módulo: recusado com 400 (curso incompleto)
        espera(400, "POST", "/api/parceira/cursos/" + id + "/revisao", null, parceira);
    }

    @Test
    void parceiraSuspensaLeMasNaoEscreve() throws Exception {
        espera(200, "GET", "/api/parceira/perfil", null, parceiraSuspensa);
        espera(200, "GET", "/api/parceira/cursos", null, parceiraSuspensa);
        espera(403, "POST", "/api/parceira/cursos", cursoJson("Curso Z"), parceiraSuspensa);
    }
}
