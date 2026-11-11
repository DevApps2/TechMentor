package com.techmentor.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Todo LocalDateTime que sai da API ganha o fuso: "2026-11-10T14:00:00-03:00".
 * O banco guarda a hora do relógio do servidor (que roda no fuso do TechMentor, veja ApiApplication);
 * aqui dizemos em qual fuso essa hora está, para o app converter para o relógio do celular.
 */
@JacksonComponent
public class DataHoraSerializador extends ValueSerializer<LocalDateTime> {

    // ssXXX: sempre com segundos e com o deslocamento (-03:00), sem frações
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final ZoneId fuso;

    public DataHoraSerializador(@Value("${app.fuso:America/Sao_Paulo}") String fuso) {
        this.fuso = ZoneId.of(fuso);
    }

    @Override
    public void serialize(LocalDateTime valor, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeString(FORMATO.format(valor.atZone(fuso)));
    }
}
