package com.techmentor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Carrega o data.sql real num H2 próprio e confere que roda sem erro, sem duplicar e que a API enxerga os dados. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:seed-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always",
        "spring.sql.init.encoding=UTF-8"
})
class SeedDataTest {

    private static final Path DATA_SQL = Path.of("src/main/resources/data.sql");

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    private String get(String caminho) throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, r.statusCode(), caminho + " -> " + r.body());
        return r.body();
    }

    private int total(String tabela) {
        return jdbc.queryForObject("select count(*) from " + tabela, Integer.class);
    }

    @Test
    void seedCarregaSemDuplicarEExpoeOsDadosDoPrototipo() throws Exception {
        List<String> tabelas = List.of("usuarios", "cursos", "modulos", "aulas", "conquistas", "desafios",
                "oportunidades", "conteudos_seguranca", "areas_atuacao", "usuario_areas_interesse");
        List<Integer> antes = tabelas.stream().map(this::total).toList();
        assertTrue(antes.stream().allMatch(n -> n > 0), "tabelas vazias após o seed: " + antes);

        // segunda execução do mesmo script: nada pode ser duplicado
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new FileSystemResource(DATA_SQL));
        populator.setSqlScriptEncoding("UTF-8"); // sem isso o Windows lê os acentos errado e "duplica" as linhas
        populator.execute(dataSource);
        List<Integer> depois = tabelas.stream().map(this::total).toList();
        assertEquals(antes, depois, "o seed não é idempotente");

        String cursos = get("/cursos");
        assertTrue(cursos.contains("UX/UI na prática"), cursos);
        assertTrue(cursos.contains("\"categoria\":\"SEGURANCA\""), cursos);
        assertTrue(cursos.contains("\"categoria\":\"REDES\""), cursos);
        assertTrue(cursos.contains("\"nivel\":\"INTERMEDIARIO\""), cursos);

        String oportunidades = get("/api/oportunidades");
        assertTrue(oportunidades.contains("\"area\":\"Programação\""), oportunidades);
        assertTrue(oportunidades.contains("\"beneficios\":\"Mentoria e horário flexível.\""), oportunidades);

        String conteudos = get("/api/seguranca/conteudos");
        assertTrue(conteudos.contains("Se algo incomodar"), conteudos);
        assertTrue(conteudos.contains("\"contato\":\"190\""), conteudos);

        // conquistas que o sistema desbloqueia sozinho precisam existir no cadastro com o nome exato
        String conquistas = get("/conquistas");
        for (String nome : List.of("Primeira aula", "Curso concluído", "Sequência de 3 dias",
                "Primeira mentoria", "Primeiro desafio", "Explorando oportunidades")) {
            assertTrue(conquistas.contains("\"nome\":\"" + nome + "\""), "faltou a conquista " + nome);
        }

        assertTrue(get("/api/mentoras?area=IA").contains("Juliana Costa"));
        assertTrue(get("/api/mentoras?busca=maria").contains("Maria Silva"));
    }

    @Test
    void usuariosDoSeedEntramComASenhaDeTeste() throws Exception {
        Matcher m = Pattern.compile("senha de teste:\\s*([^\\s]+)").matcher(Files.readString(DATA_SQL));
        assertTrue(m.find(), "o data.sql precisa documentar a senha de teste no cabeçalho");

        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/usuarios/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"email\":\"daniela.martins@email.com\",\"senha\":\"" + m.group(1) + "\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, r.statusCode(), r.body());
        assertTrue(r.body().contains("\"xpTotal\":360"), r.body());
        assertTrue(r.body().contains("\"nivel\":2"), r.body());
    }
}
