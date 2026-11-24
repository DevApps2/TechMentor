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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O formulário do site é público e a fila é só da administradora. Por HTTP de verdade, com a SecurityConfig inteira:
 * sem login 401, com login mas sem permissão 403. Cada chamada pública usa um IP de mentira (X-Forwarded-For)
 * para o limite por endereço não misturar um teste com o outro.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PedidoParceriaAcessoTest {

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

    private record Resp(int status, String corpo) { }

    @BeforeEach
    void criarPersonagens() {
        aluna = conta(Usuario.Tipo.ALUNA);
        mentora = conta(Usuario.Tipo.MENTORA);
        admin = conta(Usuario.Tipo.ADMIN);
        parceira = conta(Usuario.Tipo.PARCEIRA);

        Parceira p = new Parceira();
        p.setUsuario(parceira);
        p.setNome("Instituto " + UUID.randomUUID().toString().substring(0, 8));
        p.setTipo(TipoParceira.ONG);
        p.setResponsavel("Responsável");
        p.setSituacao(SituacaoParceira.ATIVA);
        parceiras.save(p);
    }

    private Usuario conta(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail(tipo.name().toLowerCase() + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private Resp chamar(String metodo, String caminho, String json, Usuario quem) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "10." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
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

    private static String pedidoJson(String email) {
        return "{\"nome\":\"Instituto " + UUID.randomUUID().toString().substring(0, 8) + "\",\"tipo\":\"ONG\","
                + "\"responsavel\":\"Marina Duarte\",\"email\":\"" + email + "\","
                + "\"mensagem\":\"Temos doze cursos de lógica e web em vídeo, gratuitos.\","
                + "\"aceitaPrivacidade\":true,\"versaoPolitica\":\"2026-11-18\"}";
    }

    // ---------------------------------------------------------------- o formulário é público

    @Test
    void qualquerPessoaPodeEnviarOPedidoSemLogin() throws Exception {
        Resp r = chamar("POST", "/parceiras/pedidos", pedidoJson("site" + UUID.randomUUID().toString().substring(0, 8) + "@instituto.org"), null);

        assertEquals(201, r.status(), r.corpo());
        assertTrue(r.corpo().contains("Recebemos"), r.corpo());
    }

    @Test
    void semDadosOuComDadosInvalidosOFormularioResponde400() throws Exception {
        espera(400, "POST", "/parceiras/pedidos", "{}", null);
        espera(400, "POST", "/parceiras/pedidos",
                pedidoJson("isso-nao-e-email"), null);
    }

    @Test
    void oRoboQueNaoPrecisaAprenderNadaRecebeOMesmo201() throws Exception {
        String comArmadilha = pedidoJson("robo" + UUID.randomUUID().toString().substring(0, 8) + "@spam.com")
                .replace("}", ",\"website\":\"https://spam.example\"}");

        espera(201, "POST", "/parceiras/pedidos", comArmadilha, null);
    }

    // ---------------------------------------------------------------- a fila é só da administradora

    @Test
    void aFilaESuasDecisoesSaoSoDaAdministradora() throws Exception {
        String lista = "/api/admin/pedidos-parceria";

        espera(401, "GET", lista, null, null);
        espera(403, "GET", lista, null, aluna);
        espera(403, "GET", lista, null, mentora);
        espera(403, "GET", lista, null, parceira);
        espera(200, "GET", lista, null, admin);

        for (Usuario intruso : new Usuario[]{aluna, mentora, parceira}) {
            espera(403, "GET", lista + "/1", null, intruso);
            espera(403, "POST", lista + "/1/aprovacao", null, intruso);
            espera(403, "POST", lista + "/1/recusa", "{\"motivo\":\"Não.\"}", intruso);
            espera(403, "DELETE", lista + "/1", null, intruso);
        }
        espera(401, "POST", lista + "/1/aprovacao", null, null);
        espera(401, "DELETE", lista + "/1", null, null);

        // a administradora chega até a regra de negócio
        espera(404, "GET", lista + "/999999", null, admin);
        espera(400, "POST", lista + "/999999/recusa", "{}", admin);
        espera(404, "DELETE", lista + "/999999", null, admin);
    }

    @Test
    void aAdministradoraApagaOPedidoPeloSiteEElePassaADarNaoEncontrado() throws Exception {
        String email = "apagar" + UUID.randomUUID().toString().substring(0, 8) + "@instituto.org";
        espera(201, "POST", "/parceiras/pedidos", pedidoJson(email), null);
        Resp fila = chamar("GET", "/api/admin/pedidos-parceria?situacao=PENDENTE", null, admin);
        String corpo = fila.corpo();
        int posicaoDoEmail = corpo.indexOf(email);
        int posicaoDoId = corpo.lastIndexOf("\"id\":", posicaoDoEmail) + 5;
        String id = corpo.substring(posicaoDoId, corpo.indexOf(',', posicaoDoId));

        espera(204, "DELETE", "/api/admin/pedidos-parceria/" + id, null, admin);
        espera(404, "GET", "/api/admin/pedidos-parceria/" + id, null, admin);
    }

    @Test
    void pedidoEnviadoPeloSiteApareceNaFilaDaAdministradora() throws Exception {
        String email = "fila" + UUID.randomUUID().toString().substring(0, 8) + "@instituto.org";
        espera(201, "POST", "/parceiras/pedidos", pedidoJson(email), null);

        Resp fila = chamar("GET", "/api/admin/pedidos-parceria?situacao=PENDENTE", null, admin);

        assertEquals(200, fila.status(), fila.corpo());
        assertTrue(fila.corpo().contains(email), "o pedido enviado pelo site tem que aparecer na fila");
    }
}
