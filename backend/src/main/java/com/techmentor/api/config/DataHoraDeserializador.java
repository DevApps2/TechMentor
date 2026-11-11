package com.techmentor.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * O que a API aceita de data e hora:
 * - com fuso ("2026-11-10T14:00:00-03:00" ou "...Z"): convertida para o fuso do TechMentor;
 * - sem fuso ("2026-11-10T14:00:00"): vale como a hora do relógio do TechMentor (como era antes).
 */
@JacksonComponent
public class DataHoraDeserializador extends ValueDeserializer<LocalDateTime> {

    private static final Pattern TERMINA_COM_FUSO = Pattern.compile("(Z|[+-]\\d{2}:\\d{2})$");

    private final ZoneId fuso;

    public DataHoraDeserializador(@Value("${app.fuso:America/Sao_Paulo}") String fuso) {
        this.fuso = ZoneId.of(fuso);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
        String texto = p.getString().trim();
        try {
            if (TERMINA_COM_FUSO.matcher(texto).find()) {
                return OffsetDateTime.parse(texto).atZoneSameInstant(fuso).toLocalDateTime();
            }
            return LocalDateTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw ctxt.weirdStringException(texto, LocalDateTime.class,
                    "use o formato 2026-11-10T14:00:00-03:00");
        }
    }
}
