package com.techmentor.api.dto;

import com.techmentor.api.model.Mentoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

public final class MentoriaDtos {
    private MentoriaDtos() { }
    public record AreaRequest(@NotBlank @Size(max = 80) String nome) { }
    public record AreaResponse(Long id, String nome) { }
    public record MentoraRequest(@Size(max = 2000) String bio,
                                 @Size(max = 255) String cargo,
                                 @Size(max = 255) String empresa,
                                 @PositiveOrZero Integer anosExperiencia,
                                 Set<@Positive Long> areasIds) { }
    public record MentoraResponse(Long id, Long usuarioId, String nome, String fotoUrl, String bio, String cargo,
                                  String empresa, int anosExperiencia, double notaMedia, int totalAvaliacoes, Set<String> areas) { }
    public record HorarioRequest(@NotNull LocalDateTime inicio, @NotNull LocalDateTime fim) { }
    public record HorarioResponse(Long id, Long mentoraId, LocalDateTime inicio, LocalDateTime fim, boolean reservado) { }
    public record AgendamentoRequest(@NotNull @Positive Long horarioId, @Size(max = 500) String assunto) { }
    public record AgendamentoResponse(Long id, Long alunaId, String alunaNome, Long mentoraId, String mentoraNome,
                                      Long horarioId, LocalDateTime inicio, LocalDateTime fim, Mentoria.Status status,
                                      String assunto, LocalDateTime criadaEm) { }
}
