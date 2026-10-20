package com.techmentor.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
@JsonPropertyOrder({"id", "nome", "email", "fotoUrl", "tipo", "xpTotal", "nivel", "sequenciaDias", "ultimaAtividade", "criadoEm"})
public class Usuario {
    public enum Tipo { ALUNA, MENTORA, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false, length = 120) private String nome;

    @Column(nullable = false, unique = true, length = 160) private String email;

    // WRITE_ONLY: entra no JSON de cadastro, mas nunca sai nas respostas
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false) private String senhaHash; // guardar com BCrypt, nunca a senha pura

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Tipo tipo = Tipo.ALUNA;
    private String fotoUrl;

    // Gamificação (home: "1.240 XP", "Nível 7", "5 dias de sequência")
    private int xpTotal = 0;
    private int nivel = 1;
    private int sequenciaDias = 0;
    private LocalDate ultimaAtividade;

    @Column(updatable = false) private LocalDateTime criadoEm = LocalDateTime.now();
}