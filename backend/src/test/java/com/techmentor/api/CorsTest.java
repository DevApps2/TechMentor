package com.techmentor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O portal (e só ele) pode chamar a API pelo navegador. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsTest {

    @Value("${local.server.port}")
    private int porta;

    private HttpResponse<String> preflight(String origem) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/usuarios/login"))
                .header("Origin", origem)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        return HttpClient.newHttpClient().send(pedido, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void oPortalLocalPodeChamarAApi() throws Exception {
        HttpResponse<String> r = preflight("http://localhost:5173");

        assertEquals(200, r.statusCode());
        assertEquals("http://localhost:5173", r.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertTrue(r.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase().contains("authorization"));
    }

    @Test
    void outroSiteNaoPodeChamarAApi() throws Exception {
        HttpResponse<String> r = preflight("https://site-qualquer.com");

        assertEquals(403, r.statusCode());
        assertTrue(r.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }
}