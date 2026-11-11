package com.techmentor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Todo erro da API sai no mesmo formato: {"erro": "...", "codigo": "...", e "campos" só na validação}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContratoDeErroTest {

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> chamar(String metodo, String caminho, String json) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json")
                .method(metodo, json == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void semTokenDevolve401ComCodigo() throws Exception {
        HttpResponse<String> r = chamar("GET", "/notificacoes/usuarios/1", null);

        assertEquals(401, r.statusCode());
        assertTrue(r.body().contains("\"codigo\":\"NAO_AUTENTICADO\""), r.body());
        assertTrue(r.body().contains("\"erro\":"), r.body());
    }

    @Test
    void dadosInvalidosDevolvemOCampoQueErrou() throws Exception {
        HttpResponse<String> r = chamar("POST", "/usuarios", "{\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"1\"}");

        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("\"codigo\":\"DADOS_INVALIDOS\""), r.body());
        assertTrue(r.body().contains("\"campos\":{"), r.body());
        assertTrue(r.body().contains("\"email\":"), r.body());
    }

    @Test
    void corpoQueNaoEhJsonDevolve400SemCampos() throws Exception {
        HttpResponse<String> r = chamar("POST", "/usuarios", "isto não é json");

        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("\"codigo\":\"REQUISICAO_INVALIDA\""), r.body());
        assertFalse(r.body().contains("campos"), r.body());   // sem "campos" quando não há campo para apontar
    }
}
