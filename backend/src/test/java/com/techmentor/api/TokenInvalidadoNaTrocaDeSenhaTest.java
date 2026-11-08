package com.techmentor.api;

import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Trocar a senha derruba os tokens que já estavam por aí; entrar de novo com a senha nova funciona. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenInvalidadoNaTrocaDeSenhaTest {

    private static final String SENHA_ANTIGA = "senhaSegura123";
    private static final String SENHA_NOVA = "outraSenha456";

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private record Resp(int status, String corpo) { }

    private Resp chamar(String metodo, String caminho, String json, String token) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json");
        if (token != null) req.header("Authorization", "Bearer " + token);
        req.method(metodo, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        HttpResponse<String> r = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Resp(r.statusCode(), r.body());
    }

    private static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\"\\s*:\\s*(\"[^\"]*\"|[0-9.]+)").matcher(json);
        assertTrue(m.find(), "campo '" + nome + "' não encontrado em: " + json);
        return m.group(1).replace("\"", "");
    }

    private String entrar(String email, String senha) throws Exception {
        Resp login = chamar("POST", "/usuarios/login", "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}", null);
        assertEquals(200, login.status(), login.corpo());
        return campo(login.corpo(), "accessToken");
    }

    @Test
    void trocarASenhaInvalidaOTokenAntigoMasONovoLoginFunciona() throws Exception {
        String email = "tk" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
        Resp cadastro = chamar("POST", "/usuarios",
                "{\"nome\":\"Ana Token\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA_ANTIGA + "\"}", null);
        assertEquals(201, cadastro.status(), cadastro.corpo());
        String id = campo(cadastro.corpo(), "id");

        String tokenAntigo = entrar(email, SENHA_ANTIGA);
        assertEquals(200, chamar("GET", "/usuarios/" + id, null, tokenAntigo).status());

        Resp troca = chamar("PATCH", "/usuarios/" + id + "/senha",
                "{\"senhaAtual\":\"" + SENHA_ANTIGA + "\",\"novaSenha\":\"" + SENHA_NOVA + "\"}", tokenAntigo);
        assertEquals(204, troca.status(), troca.corpo());

        // o token de antes da troca não vale mais
        Resp antigoDepois = chamar("GET", "/usuarios/" + id, null, tokenAntigo);
        assertEquals(401, antigoDepois.status(), antigoDepois.corpo());

        // entrando de novo com a senha nova, o token novo vale
        String tokenNovo = entrar(email, SENHA_NOVA);
        assertEquals(200, chamar("GET", "/usuarios/" + id, null, tokenNovo).status());
    }
}
