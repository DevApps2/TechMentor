package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
public class Usuario {
    public enum Tipo { ALUNA, MENTORA, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String nome;
    @Column(nullable = false, unique = true, length = 160) private String email;
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
