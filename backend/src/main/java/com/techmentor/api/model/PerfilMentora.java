package com.techmentor.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;
import java.util.*;

@Entity @Table(name = "perfis_mentora")
@Getter @Setter @NoArgsConstructor
public class PerfilMentora {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(length = 2000) private String bio;
    private String cargo;
    private String empresa;
    private int anosExperiencia;
    private double notaMedia = 0;
    private int totalAvaliacoes = 0;

    @ManyToMany
    @JoinTable(name = "mentora_areas",
            joinColumns = @JoinColumn(name = "perfil_mentora_id"),
            inverseJoinColumns = @JoinColumn(name = "area_id"))
    private Set<AreaAtuacao> areas = new HashSet<>();
}
