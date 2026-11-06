package com.techmentor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.techmentor.api.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;

/** Sobe a API de verdade (Tomcat em porta aleatória, banco H2 em memória) e chama os endpoints por HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegracaoTest {

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    private record Resp(int status, String corpo) { }

    private String logadoId;

    private Resp chamar(String metodo, String caminho, String json, String usuarioId) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json");
        String quem = usuarioId != null ? usuarioId : logadoId;
        if (quem != null) req.header("Authorization", "Bearer " + jwtService.gerarToken(quem));
        req.method(metodo, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        HttpResponse<String> r = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Resp(r.statusCode(), r.body());
    }

    private Resp get(String caminho) throws Exception { return chamar("GET", caminho, null, null); }

    private Resp post(String caminho, String json) throws Exception { return chamar("POST", caminho, json, null); }

    private static String enc(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\"\\s*:\\s*(\"[^\"]*\"|[0-9.]+|true|false|null)").matcher(json);
        assertTrue(m.find(), "campo '" + nome + "' não encontrado em: " + json);
        return m.group(1).replace("\"", "");
    }

    private String criarUsuario(String nome) throws Exception {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
        Resp r = post("/usuarios", "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"senhaSegura123\"}");
        assertEquals(201, r.status(), r.corpo());
        String id = campo(r.corpo(), "id");
        logadoId = id;
        return id;
    }

    @Test
    void cadastroLoginEErros() throws Exception {
        String email = "ana" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
        String corpo = "{\"nome\":\"Ana Teste\",\"email\":\"" + email + "\",\"senha\":\"senhaSegura123\"}";

        Resp criado = post("/usuarios", corpo);
        assertEquals(201, criado.status(), criado.corpo());
        assertEquals("1", campo(criado.corpo(), "nivel"));
        assertEquals("Curiosa Tech", campo(criado.corpo(), "tituloNivel"));
        assertEquals("250", campo(criado.corpo(), "xpProximoNivel"));

        Resp repetido = post("/usuarios", corpo);
        assertEquals(409, repetido.status());
        assertTrue(repetido.corpo().contains("\"erro\""), repetido.corpo());

        Resp login = post("/usuarios/login", "{\"email\":\"" + email + "\",\"senha\":\"senhaSegura123\"}");
        assertEquals(200, login.status(), login.corpo());

        String token = campo(login.corpo(), "accessToken");

        assertTrue(token.length() > 20, "JWT não foi gerado corretamente");
        assertEquals("Bearer", campo(login.corpo(), "tokenType"));
        assertEquals("900", campo(login.corpo(), "expiresIn"));

        Resp senhaErrada = post("/usuarios/login", "{\"email\":\"" + email + "\",\"senha\":\"errada\"}");
        assertEquals(401, senhaErrada.status());
        assertTrue(senhaErrada.corpo().contains("Credenciais inválidas"), senhaErrada.corpo());

        Resp semEmail = post("/usuarios/login", "{\"email\":\"\",\"senha\":\"\"}");
        assertEquals(400, semEmail.status());
        assertTrue(semEmail.corpo().contains("\"erro\""), semEmail.corpo());
    }

    @Test
    void cabecalhoAntigoNaoIdentificaMais() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/agendamentos"))
                .header("X-Usuario-Id", "1")
                .GET()
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(401, r.statusCode());
    }

    @Test
    void perfilBuscaEAtualizacao() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 6);
        String id = criarUsuario("Zuleica Busca" + sufixo);
        criarUsuario("Outra Pessoa " + sufixo);   // logada = outra pessoa: quem busca nunca aparece no próprio resultado

        Resp busca = get("/usuarios/busca?nome=" + enc("Zuleica Busca" + sufixo));
        assertEquals(200, busca.status());
        assertTrue(busca.corpo().contains("Zuleica Busca" + sufixo), busca.corpo());
        assertTrue(!busca.corpo().contains("@"), "a busca não pode expor e-mail: " + busca.corpo());

        assertEquals(400, get("/usuarios/busca?nome=a").status());

        logadoId = id;   // agora volta a ser a Zuleica: só a dona (ou admin) vê e edita o próprio perfil

        Resp atual = get("/usuarios/" + id);
        String email = campo(atual.corpo(), "email");
        Resp put = chamar("PUT", "/usuarios/" + id,
                "{\"nome\":\"Zuleica Nova\",\"email\":\"" + email + "\",\"fotoUrl\":null,"
                        + "\"bio\":\"Estudante\",\"areasInteresse\":[\"Desenvolvimento Web\",\"Dados\"]}", null);
        assertEquals(200, put.status(), put.corpo());
        assertEquals("Estudante", campo(put.corpo(), "bio"));
        assertTrue(put.corpo().contains("Desenvolvimento Web"), put.corpo());

        // sem bio/áreas no corpo, o que já existe é mantido
        Resp semCampos = chamar("PUT", "/usuarios/" + id,
                "{\"nome\":\"Zuleica Nova\",\"email\":\"" + email + "\",\"fotoUrl\":null}", null);
        assertEquals("Estudante", campo(semCampos.corpo(), "bio"));
    }

    @Test
    void trilhaComBarraNoTituloEstadoDasAulasENivel() throws Exception {
        String id = criarUsuario("Aluna Trilha");
        String titulo = "UX/UI na prática " + UUID.randomUUID().toString().substring(0, 6);
        String t = enc(titulo);

        Resp conquistaCriada = post("/conquistas",
                "{\"nome\":\"Primeira aula\",\"descricao\":\"d\",\"iconeUrl\":null,\"xpBonus\":0,\"secreta\":false}");
        // 201 = criada agora; 409 = outro teste já criou. Qualquer outra coisa é erro de verdade
        assertTrue(conquistaCriada.status() == 201 || conquistaCriada.status() == 409, conquistaCriada.corpo());

        Resp curso = chamar("POST", "/cursos", "{\"titulo\":\"" + titulo + "\",\"descricao\":\"x\","
                + "\"categoria\":\"SEGURANCA\",\"nivel\":\"INTERMEDIARIO\"}", criarAdmin());
        assertEquals(201, curso.status(), curso.corpo());
        assertEquals("INTERMEDIARIO", campo(curso.corpo(), "nivel"));

        // %2F no caminho: precisa chegar ao controller como "/" no título
        Resp modulo = post("/modulos/" + t, "{\"titulo\":\"Intro\",\"ordem\":1}");
        assertEquals(201, modulo.status(), modulo.corpo());
        assertEquals(201, post("/aulas/" + t + "/1",
                "{\"titulo\":\"Aula 1\",\"ordem\":1,\"duracaoMinutos\":10,\"xpRecompensa\":300,\"conteudoUrl\":null}").status());
        assertEquals(201, post("/aulas/" + t + "/1",
                "{\"titulo\":\"Aula 2\",\"ordem\":2,\"duracaoMinutos\":5,\"xpRecompensa\":20,\"conteudoUrl\":null}").status());

        Resp detalhe = get("/cursos/buscar?titulo=" + t);
        assertEquals(200, detalhe.status(), detalhe.corpo());
        assertEquals("15", campo(detalhe.corpo(), "duracaoTotalMinutos"));
        assertEquals("320", campo(detalhe.corpo(), "xpTotal"));

        Resp antes = get("/progresso/" + t + "/usuarios/" + id + "/aulas");
        assertEquals(200, antes.status(), antes.corpo());
        assertTrue(antes.corpo().indexOf("ATUAL") < antes.corpo().indexOf("BLOQUEADA"), antes.corpo());

        Resp foraDeOrdem = post("/progresso/" + t + "/1/2/usuarios/" + id, null);
        assertEquals(409, foraDeOrdem.status());
        assertTrue(foraDeOrdem.corpo().contains("anteriores"), foraDeOrdem.corpo());

        // sem matrícula prévia: concluir a primeira aula matricula sozinha
        Resp concluida = post("/progresso/" + t + "/1/1/usuarios/" + id, null);
        assertEquals(201, concluida.status(), concluida.corpo());

        Resp depois = get("/progresso/" + t + "/usuarios/" + id + "/aulas");
        assertTrue(depois.corpo().indexOf("CONCLUIDA") < depois.corpo().indexOf("ATUAL"), depois.corpo());

        Resp matriculas = get("/matriculas/usuarios/" + id);
        assertEquals("1", campo(matriculas.corpo(), "aulasConcluidas"));
        assertEquals("2", campo(matriculas.corpo(), "totalAulas"));
        assertEquals("50", campo(matriculas.corpo(), "percentualConcluido"));

        // 300 XP da aula: sobe para o nível 2 e a sequência começa em 1 dia
        Resp usuario = get("/usuarios/" + id);
        assertEquals("2", campo(usuario.corpo(), "nivel"));
        assertEquals("1", campo(usuario.corpo(), "sequenciaDias"));
        assertEquals("500", campo(usuario.corpo(), "xpProximoNivel"));

        Resp conquistas = get("/conquistas/usuarios/" + id);
        assertTrue(conquistas.corpo().contains("Primeira aula"), conquistas.corpo());
        assertTrue(get("/notificacoes/usuarios/" + id).corpo().contains("Você subiu de nível!"));
    }

    @Test
    void denunciaConteudosDeSegurancaEFiltros() throws Exception {
        String id = criarUsuario("Denunciante");

        Resp curta = chamar("POST", "/api/denuncias",
                "{\"usuarioDenunciadoId\":null,\"motivo\":\"OUTRO\",\"descricao\":\"curta\"}", id);
        assertEquals(400, curta.status());
        assertTrue(curta.corpo().contains("entre 10 e 2000"), curta.corpo());

        Resp valida = chamar("POST", "/api/denuncias",
                "{\"usuarioDenunciadoId\":null,\"motivo\":\"COMPORTAMENTO_INADEQUADO\","
                        + "\"descricao\":\"Mensagens ofensivas no chat\"}", id);
        assertEquals(201, valida.status(), valida.corpo());

        assertEquals(200, get("/api/seguranca/conteudos").status());
        assertEquals(200, get("/api/seguranca/conteudos?tipo=CANAL_AJUDA").status());

        assertEquals(200, get("/api/oportunidades?tipo=ESTAGIO&area=Programa%C3%A7%C3%A3o").status());
        assertEquals(200, get("/api/mentoras?busca=java&area=Dados").status());
        assertEquals(200, get("/desafios").status());
    }

    @Test
    void acessoProtegidoComJwt() throws Exception {
        String email = "jwt" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";

        Resp cadastro = post(
                "/usuarios",
                "{\"nome\":\"Teste JWT\",\"email\":\"" + email + "\",\"senha\":\"senhaSegura123\"}"
        );

        assertEquals(201, cadastro.status(), cadastro.corpo());

        Resp login = post(
                "/usuarios/login",
                "{\"email\":\"" + email + "\",\"senha\":\"senhaSegura123\"}"
        );

        assertEquals(200, login.status(), login.corpo());

        String token = campo(login.corpo(), "accessToken");

        Resp semToken = get("/usuarios");
        assertEquals(401, semToken.status(), semToken.corpo());

        Resp comToken = getComToken("/usuarios", token);
        assertEquals(200, comToken.status(), comToken.corpo());

        Resp tokenInvalido = getComToken("/usuarios", "token_invalido");
        assertEquals(401, tokenInvalido.status(), tokenInvalido.corpo());
    }

    private Resp getComToken(String caminho, String token) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + caminho)
                )
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> r = http.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        return new Resp(r.statusCode(), r.body());
    }

    /** Cria um administrador direto no banco (não existe rota pública para isso). */
    private String criarAdmin() {
        Usuario admin = new Usuario();
        admin.setNome("Admin Teste");
        admin.setEmail("admin" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        admin.setSenhaHash("nao-usada-nos-testes");
        admin.setTipo(Usuario.Tipo.ADMIN);
        return usuarioRepository.save(admin).getId().toString();
    }
}
