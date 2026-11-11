package com.techmentor.api;

import com.techmentor.api.config.DataHoraDeserializador;
import com.techmentor.api.config.DataHoraSerializador;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** As datas saem da API com o fuso ("-03:00") e entram com ou sem ele, sempre no relógio do TechMentor. */
class FusoHorarioTest {

    private final JsonMapper json = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addSerializer(LocalDateTime.class, new DataHoraSerializador("America/Sao_Paulo"))
                    .addDeserializer(LocalDateTime.class, new DataHoraDeserializador("America/Sao_Paulo")))
            .build();

    @Test
    void saiComOFusoESemFracoes() {
        LocalDateTime hora = LocalDateTime.of(2026, 11, 10, 14, 0, 0, 123_456_000);

        assertEquals("\"2026-11-10T14:00:00-03:00\"", json.writeValueAsString(hora));
    }

    @Test
    void entraSemFusoComoHoraDoTechMentor() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 14, 0), json.readValue("\"2026-11-10T14:00:00\"", LocalDateTime.class));
    }

    @Test
    void entraComFusoConvertidoParaOFusoDoTechMentor() {
        // 17:00 em UTC = 14:00 em São Paulo; 10:00 em Manaus (-04:00) = 11:00 em São Paulo
        assertEquals(LocalDateTime.of(2026, 11, 10, 14, 0), json.readValue("\"2026-11-10T17:00:00Z\"", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 11, 10, 11, 0), json.readValue("\"2026-11-10T10:00:00-04:00\"", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 11, 10, 14, 0), json.readValue("\"2026-11-10T14:00:00-03:00\"", LocalDateTime.class));
    }

    @Test
    void textoQueNaoEhDataEhRejeitado() {
        assertThrows(JacksonException.class, () -> json.readValue("\"amanhã\"", LocalDateTime.class));
    }
}
