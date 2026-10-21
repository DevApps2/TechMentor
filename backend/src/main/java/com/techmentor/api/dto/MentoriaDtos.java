package com.techmentor.api.dto;

import com.techmentor.api.model.Mentoria;
import java.time.LocalDateTime;
import java.util.Set;

public final class MentoriaDtos {
    private MentoriaDtos() { }
    public record AreaRequest(String nome) { }
    public record AreaResponse(Long id, String nome) { }
    public record MentoraRequest(String bio, String cargo, String empresa, Integer anosExperiencia, Set<Long> areasIds) { }
    public record MentoraResponse(Long id, Long usuarioId, String nome, String fotoUrl, String bio, String cargo,
                                  String empresa, int anosExperiencia, double notaMedia, int totalAvaliacoes, Set<String> areas) { }
    public record HorarioRequest(LocalDateTime inicio, LocalDateTime fim) { }
    public record HorarioResponse(Long id, Long mentoraId, LocalDateTime inicio, LocalDateTime fim, boolean reservado) { }
    public record AgendamentoRequest(Long horarioId, String assunto) { }
    public record AgendamentoResponse(Long id, Long alunaId, String alunaNome, Long mentoraId, String mentoraNome,
                                      Long horarioId, LocalDateTime inicio, LocalDateTime fim, Mentoria.Status status,
                                      String assunto, LocalDateTime criadaEm) { }
}
