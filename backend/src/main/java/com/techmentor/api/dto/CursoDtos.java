package com.techmentor.api.dto;

import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Aula;
import com.techmentor.api.model.Matricula;
import com.techmentor.api.model.Modulo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTOs usados pelos endpoints de cursos, módulos e aulas. */
public final class CursoDtos {
    private CursoDtos() { }

    public record CursoRequest(
            @NotBlank @Size(max = 150) String titulo,
            @Size(max = 1000) String descricao,
            @NotNull Curso.Categoria categoria,
            @Size(max = 255) String iconeUrl,
            Boolean ativo
    ) {
        public Curso toEntity() {
            Curso curso = new Curso();
            curso.setTitulo(titulo);
            curso.setDescricao(descricao);
            curso.setCategoria(categoria);
            curso.setIconeUrl(iconeUrl);
            if (ativo != null) curso.setAtivo(ativo);
            return curso;
        }
    }

    public record CursoResponse(
            Long id,
            String titulo,
            String descricao,
            Curso.Categoria categoria,
            String iconeUrl,
            boolean ativo,
            Long totalAulas,
            List<ModuloResponse> modulos
    ) {
        public static CursoResponse from(Curso curso) {
            List<ModuloResponse> modulos = curso.getModulos() == null ? List.of()
                    : curso.getModulos().stream().map(ModuloResponse::from).toList();
            return new CursoResponse(curso.getId(), curso.getTitulo(), curso.getDescricao(),
                    curso.getCategoria(), curso.getIconeUrl(), curso.isAtivo(), curso.getTotalAulas(), modulos);
        }
    }

    public record ModuloRequest(
            @NotBlank String titulo,
            @NotNull @PositiveOrZero Integer ordem
    ) {
        public Modulo toEntity() {
            Modulo modulo = new Modulo();
            modulo.setTitulo(titulo);
            modulo.setOrdem(ordem);
            return modulo;
        }
    }

    public record ModuloDeleteRequest(@NotNull @PositiveOrZero Integer ordem) { }

    public record ModuloResponse(
            Long id,
            String titulo,
            int ordem,
            List<AulaResponse> aulas
    ) {
        public static ModuloResponse from(Modulo modulo) {
            List<AulaResponse> aulas = modulo.getAulas() == null ? List.of()
                    : modulo.getAulas().stream().map(AulaResponse::from).toList();
            return new ModuloResponse(modulo.getId(), modulo.getTitulo(), modulo.getOrdem(), aulas);
        }
    }

    public record AulaRequest(
            @NotBlank String titulo,
            @NotNull @PositiveOrZero Integer ordem,
            @PositiveOrZero int duracaoMinutos,
            @PositiveOrZero Integer xpRecompensa,
            @Size(max = 255) String conteudoUrl
    ) {
        public Aula toEntity() {
            Aula aula = new Aula();
            aula.setTitulo(titulo);
            aula.setOrdem(ordem);
            aula.setDuracaoMinutos(duracaoMinutos);
            if (xpRecompensa != null) aula.setXpRecompensa(xpRecompensa);
            aula.setConteudoUrl(conteudoUrl);
            return aula;
        }
    }

    public record AulaResponse(
            Long id,
            String titulo,
            int ordem,
            int duracaoMinutos,
            int xpRecompensa,
            String conteudoUrl
    ) {
        public static AulaResponse from(Aula aula) {
            return new AulaResponse(aula.getId(), aula.getTitulo(), aula.getOrdem(),
                    aula.getDuracaoMinutos(), aula.getXpRecompensa(), aula.getConteudoUrl());
        }
    }

    public record MatriculaResponse(
            Long id,
            Long usuarioId,
            Long cursoId,
            String tituloCurso,
            java.time.LocalDateTime matriculadaEm,
            java.time.LocalDateTime concluidoEm
    ) {
        public static MatriculaResponse from(Matricula matricula) {
            return new MatriculaResponse(matricula.getId(), matricula.getUsuario().getId(),
                    matricula.getCurso().getId(), matricula.getCurso().getTitulo(),
                    matricula.getMatriculadaEm(), matricula.getConcluidoEm());
        }
    }
}
