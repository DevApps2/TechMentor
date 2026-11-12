package com.techmentor.api;

import com.techmentor.api.dto.CursoDtos.AulaRequest;
import com.techmentor.api.security.JwtService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Toda aula tem vídeo de verdade (https), a trilha entrega o link ao app e um link fora do padrão é recusado. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:videos-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always",
        "spring.sql.init.encoding=UTF-8"
})
class VideosDasAulasTest {

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void todaAulaDoSeedTemUmVideoHttpsQueNaoEhOLinkInventado() {
        int semVideo = jdbc.queryForObject(
                "select count(*) from aulas where conteudo_url is null or conteudo_url not like 'https://%'",
                Integer.class);
        int linkInventado = jdbc.queryForObject(
                "select count(*) from aulas where conteudo_url like 'https://cdn.techmentor.com/%'",
                Integer.class);

        assertEquals(0, semVideo, "há aula sem link https");
        assertEquals(0, linkInventado, "ainda há link inventado (cdn.techmentor.com)");
    }

    @Test
    void todasAsAulasSaoVideosDoYoutubeComIdValido() {
        int foraDoYoutube = jdbc.queryForObject(
                "select count(*) from aulas where conteudo_url not like 'https://www.youtube.com/watch?v=%'",
                Integer.class);
        int idsInvalidos = jdbc.queryForObject(
                "select count(*) from aulas where length(conteudo_url) <> length('https://www.youtube.com/watch?v=') + 11",
                Integer.class);
        int total = jdbc.queryForObject("select count(*) from aulas", Integer.class);

        assertEquals(0, foraDoYoutube, "há aula que não é vídeo do YouTube (clipe de teste?)");
        assertEquals(0, idsInvalidos, "id de vídeo do YouTube precisa ter 11 caracteres");
        assertEquals(53, total);
    }

    @Test
    void aTrilhaEntregaOLinkDoVideoDeCadaAula() throws Exception {
        Long usuarioId = jdbc.queryForObject(
                "select id from usuarios where email = 'beatriz.santos@email.com'", Long.class);

        HttpResponse<String> resposta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + porta
                                + "/progresso/Java%20para%20iniciantes/usuarios/" + usuarioId + "/aulas"))
                        .header("Authorization", "Bearer " + jwtService.gerarToken(usuarioId.toString()))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resposta.statusCode(), resposta.body());

        // o curso tem 8 aulas e cada uma traz o seu link
        int links = resposta.body().split("\"conteudoUrl\":\"https://", -1).length - 1;
        assertEquals(8, links, resposta.body());
    }

    @Test
    void linkSemHttpsEhRecusadoNoCadastroDaAula() {
        assertFalse(VALIDADOR.validate(new AulaRequest("Aula", 1, 10, 20, "http://site.com/video.mp4")).isEmpty());
        assertFalse(VALIDADOR.validate(new AulaRequest("Aula", 1, 10, 20, "video.mp4")).isEmpty());

        // https e "sem vídeo ainda" (nulo) continuam valendo
        assertTrue(VALIDADOR.validate(new AulaRequest("Aula", 1, 10, 20, "https://site.com/video.mp4")).isEmpty());
        assertTrue(VALIDADOR.validate(new AulaRequest("Aula", 1, 10, 20, null)).isEmpty());
    }
}
