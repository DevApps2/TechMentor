package com.techmentor.api;

import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

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

/** O login avisa quando a senha é provisória (o app leva a pessoa a trocá-la); depois da troca, o aviso some. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SenhaProvisoriaNoLoginTest {

    private static final String SENHA_PROVISORIA = "provisoria123";
    private static final String SENHA_NOVA = "senhaEscolhida456";

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private Resp entrar(String email, String senha) throws Exception {
        Resp login = chamar("POST", "/usuarios/login",
                "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}", null);
        assertEquals(200, login.status(), login.corpo());
        return login;
    }

    private static String texto(String json, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        assertTrue(m.find(), "campo '" + nome + "' não encontrado em: " + json);
        return m.group(1);
    }

    private static boolean precisaTrocarSenha(String json) {
        Matcher m = Pattern.compile("\"precisaTrocarSenha\"\\s*:\\s*(true|false)").matcher(json);
        assertTrue(m.find(), "campo 'precisaTrocarSenha' não encontrado em: " + json);
        return Boolean.parseBoolean(m.group(1));
    }

    @Test
    void oLoginAvisaDaSenhaProvisoriaEOAvisoSomeDepoisDaTroca() throws Exception {
        String email = "sp" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";

        // conta como a admin cadastra a mentora: com senha provisória
        Usuario conta = new Usuario();
        conta.setNome("Mentora Provisória");
        conta.setEmail(email);
        conta.setSenhaHash(passwordEncoder.encode(SENHA_PROVISORIA));
        conta.setTipo(Usuario.Tipo.MENTORA);
        conta.setSenhaProvisoria(true);
        conta = usuarios.save(conta);

        Resp primeiroLogin = entrar(email, SENHA_PROVISORIA);
        assertTrue(precisaTrocarSenha(primeiroLogin.corpo()), primeiroLogin.corpo());

        String token = texto(primeiroLogin.corpo(), "accessToken");
        Resp troca = chamar("PATCH", "/usuarios/" + conta.getId() + "/senha",
                "{\"senhaAtual\":\"" + SENHA_PROVISORIA + "\",\"novaSenha\":\"" + SENHA_NOVA + "\"}", token);
        assertEquals(204, troca.status(), troca.corpo());

        Resp loginComASenhaNova = entrar(email, SENHA_NOVA);
        assertFalse(precisaTrocarSenha(loginComASenhaNova.corpo()), loginComASenhaNova.corpo());
    }

    @Test
    void contaComumEntraSemAviso() throws Exception {
        String email = "sc" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";

        Usuario conta = new Usuario();
        conta.setNome("Aluna Comum");
        conta.setEmail(email);
        conta.setSenhaHash(passwordEncoder.encode(SENHA_PROVISORIA));
        usuarios.save(conta);

        Resp login = entrar(email, SENHA_PROVISORIA);
        assertFalse(precisaTrocarSenha(login.corpo()), login.corpo());
    }
}
