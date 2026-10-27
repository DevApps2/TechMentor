package com.techmentor.api.dto;

import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Aula;
import com.techmentor.api.model.Matricula;
import com.techmentor.api.model.Modulo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTOs usados pelos endpoints de cursos, módulos e aulas. */
public final class CursoDtos {
    private CursoDtos() { }

    @Schema(description = "Dados para cadastrar ou atualizar um curso")
    public record CursoRequest(
            @Schema(description = "Título do curso", example = "Java para iniciantes")
            @NotBlank @Size(max = 150) String titulo,
            @Schema(description = "Descrição do curso", example = "Aprenda os fundamentos de Java do zero")
            @Size(max = 1000) String descricao,
            @Schema(description = "Categoria do curso", example = "PROGRAMACAO")
            @NotNull Curso.Categoria categoria,
            @Schema(description = "URL do ícone do curso", example = "https://cdn.techmentor.com/icones/java.png")
            @Size(max = 255) String iconeUrl,
            @Schema(description = "Indica se o curso está ativo; se omitido, o curso é criado como ativo", example = "true")
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

    @Schema(description = "Curso com seus módulos e aulas")
    public record CursoResponse(
            @Schema(description = "ID do curso", example = "1")
            Long id,
            @Schema(description = "Título do curso", example = "Java para iniciantes")
            String titulo,
            @Schema(description = "Descrição do curso", example = "Aprenda os fundamentos de Java do zero")
            String descricao,
            @Schema(description = "Categoria do curso", example = "PROGRAMACAO")
            Curso.Categoria categoria,
            @Schema(description = "URL do ícone do curso", example = "https://cdn.techmentor.com/icones/java.png")
            String iconeUrl,
            @Schema(description = "Indica se o curso está ativo", example = "true")
            boolean ativo,
            @Schema(description = "Total de aulas do curso", example = "12")
            Long totalAulas,
            @Schema(description = "Módulos do curso, ordenados pela ordem")
            List<ModuloResponse> modulos
    ) {
        public static CursoResponse from(Curso curso) {
            List<ModuloResponse> modulos = curso.getModulos() == null ? List.of()
                    : curso.getModulos().stream().map(ModuloResponse::from).toList();
            return new CursoResponse(curso.getId(), curso.getTitulo(), curso.getDescricao(),
                    curso.getCategoria(), curso.getIconeUrl(), curso.isAtivo(), curso.getTotalAulas(), modulos);
        }
    }

    @Schema(description = "Dados para criar ou atualizar um módulo")
    public record ModuloRequest(
            @Schema(description = "Título do módulo", example = "Introdução")
            @NotBlank String titulo,
            @Schema(description = "Posição do módulo dentro do curso", example = "1")
            @NotNull @PositiveOrZero Integer ordem
    ) {
        public Modulo toEntity() {
            Modulo modulo = new Modulo();
            modulo.setTitulo(titulo);
            modulo.setOrdem(ordem);
            return modulo;
        }
    }

    @Schema(description = "Identifica o módulo a ser removido pelo endpoint legado de exclusão")
    public record ModuloDeleteRequest(
            @Schema(description = "Posição do módulo dentro do curso", example = "1")
            @NotNull @PositiveOrZero Integer ordem) { }

    @Schema(description = "Módulo de um curso com suas aulas")
    public record ModuloResponse(
            @Schema(description = "ID do módulo", example = "1")
            Long id,
            @Schema(description = "Título do módulo", example = "Introdução")
            String titulo,
            @Schema(description = "Posição do módulo dentro do curso", example = "1")
            int ordem,
            @Schema(description = "Aulas do módulo")
            List<AulaResponse> aulas
    ) {
        public static ModuloResponse from(Modulo modulo) {
            List<AulaResponse> aulas = modulo.getAulas() == null ? List.of()
                    : modulo.getAulas().stream().map(AulaResponse::from).toList();
            return new ModuloResponse(modulo.getId(), modulo.getTitulo(), modulo.getOrdem(), aulas);
        }
    }

    @Schema(description = "Dados para criar ou atualizar uma aula")
    public record AulaRequest(
            @Schema(description = "Título da aula", example = "Variáveis e tipos de dados")
            @NotBlank String titulo,
            @Schema(description = "Posição da aula dentro do módulo", example = "1")
            @NotNull @PositiveOrZero Integer ordem,
            @Schema(description = "Duração da aula em minutos", example = "15")
            @PositiveOrZero int duracaoMinutos,
            @Schema(description = "XP concedido ao concluir a aula; se omitido, usa o valor padrão", example = "10")
            @PositiveOrZero Integer xpRecompensa,
            @Schema(description = "URL do conteúdo da aula", example = "https://cdn.techmentor.com/aulas/variaveis.mp4")
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

    @Schema(description = "Aula de um módulo")
    public record AulaResponse(
            @Schema(description = "ID da aula", example = "1")
            Long id,
            @Schema(description = "Título da aula", example = "Variáveis e tipos de dados")
            String titulo,
            @Schema(description = "Posição da aula dentro do módulo", example = "1")
            int ordem,
            @Schema(description = "Duração da aula em minutos", example = "15")
            int duracaoMinutos,
            @Schema(description = "XP concedido ao concluir a aula", example = "10")
            int xpRecompensa,
            @Schema(description = "URL do conteúdo da aula", example = "https://cdn.techmentor.com/aulas/variaveis.mp4")
            String conteudoUrl
    ) {
        public static AulaResponse from(Aula aula) {
            return new AulaResponse(aula.getId(), aula.getTitulo(), aula.getOrdem(),
                    aula.getDuracaoMinutos(), aula.getXpRecompensa(), aula.getConteudoUrl());
        }
    }

    @Schema(description = "Matrícula de uma usuária em um curso")
    public record MatriculaResponse(
            @Schema(description = "ID da matrícula", example = "1")
            Long id,
            @Schema(description = "ID da usuária matriculada", example = "1")
            Long usuarioId,
            @Schema(description = "ID do curso", example = "1")
            Long cursoId,
            @Schema(description = "Título do curso", example = "Java para iniciantes")
            String tituloCurso,
            @Schema(description = "Data e hora da matrícula", example = "2026-10-26T14:30:00")
            java.time.LocalDateTime matriculadaEm,
            @Schema(description = "Data e hora da conclusão do curso; nulo enquanto não concluído", example = "2026-11-30T18:00:00", nullable = true)
            java.time.LocalDateTime concluidoEm
    ) {
        public static MatriculaResponse from(Matricula matricula) {
            return new MatriculaResponse(matricula.getId(), matricula.getUsuario().getId(),
                    matricula.getCurso().getId(), matricula.getCurso().getTitulo(),
                    matricula.getMatriculadaEm(), matricula.getConcluidoEm());
        }
    }
}
