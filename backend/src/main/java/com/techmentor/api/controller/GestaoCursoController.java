package com.techmentor.api.controller;

import com.techmentor.api.dto.CursoDtos.AulaResponse;
import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import com.techmentor.api.dto.GestaoCursoDtos.AulaGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.MetricasCursoResponse;
import com.techmentor.api.dto.GestaoCursoDtos.ModuloGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.OrdemRequest;
import com.techmentor.api.dto.GestaoCursoDtos.SituacaoRequest;
import com.techmentor.api.model.SituacaoCurso;
import com.techmentor.api.service.GestaoCursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestão de cursos, módulos e aulas por id. Tudo sob /api/admin: a SecurityConfig já restringe essa faixa à administradora.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Gestão de cursos (administradora)", description = "Criar e manter cursos, módulos e aulas por id, com rascunho, publicação, reordenação e métricas")
public class GestaoCursoController {

    private final GestaoCursoService gestao;

    // ---- cursos ----

    @Operation(summary = "Listar todos os cursos", description = "Inclui rascunhos e arquivados; filtro opcional por situação.")
    @GetMapping("/cursos")
    public List<CursoResponse> listar(@RequestParam(required = false) SituacaoCurso situacao) {
        return gestao.listar(situacao);
    }

    @Operation(summary = "Detalhar um curso com módulos e aulas")
    @GetMapping("/cursos/{id}")
    public CursoResponse detalhar(@Parameter(description = "ID do curso") @PathVariable Long id) {
        return gestao.detalhar(id);
    }

    @Operation(summary = "Criar curso", description = "O curso novo sempre nasce em RASCUNHO.")
    @PostMapping("/cursos")
    public ResponseEntity<CursoResponse> criar(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(201).body(gestao.criar(request));
    }

    @Operation(summary = "Editar os dados do curso", description = "A situação não muda aqui: use PATCH /cursos/{id}/situacao.")
    @PutMapping("/cursos/{id}")
    public CursoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return gestao.atualizar(id, request);
    }

    @Operation(summary = "Mudar a situação do curso",
            description = "Publicar exige curso completo; voltar a rascunho só se ninguém estiver matriculada.")
    @PatchMapping("/cursos/{id}/situacao")
    public CursoResponse mudarSituacao(@PathVariable Long id, @Valid @RequestBody SituacaoRequest request) {
        return gestao.mudarSituacao(id, request.situacao());
    }

    @Operation(summary = "Métricas do curso", description = "Matrículas, concluintes e a aula onde mais alunas pararam. Nunca identifica alunas.")
    @GetMapping("/cursos/{id}/metricas")
    public MetricasCursoResponse metricas(@PathVariable Long id) {
        return gestao.metricas(id);
    }

    // ---- módulos ----

    @Operation(summary = "Criar módulo", description = "Entra no fim do curso.")
    @PostMapping("/cursos/{id}/modulos")
    public ResponseEntity<ModuloResponse> criarModulo(@PathVariable Long id, @Valid @RequestBody ModuloGestaoRequest request) {
        return ResponseEntity.status(201).body(gestao.criarModulo(id, request));
    }

    @Operation(summary = "Reordenar os módulos do curso",
            description = "Recebe os ids de todos os módulos na nova ordem. Recusado se alguma aluna já estudou o curso.")
    @PutMapping("/cursos/{id}/modulos/ordem")
    public CursoResponse reordenarModulos(@PathVariable Long id, @Valid @RequestBody OrdemRequest request) {
        return gestao.reordenarModulos(id, request.ids());
    }

    @Operation(summary = "Editar módulo")
    @PutMapping("/modulos/{id}")
    public ModuloResponse atualizarModulo(@PathVariable Long id, @Valid @RequestBody ModuloGestaoRequest request) {
        return gestao.atualizarModulo(id, request);
    }

    @Operation(summary = "Apagar módulo", description = "Recusado se alguma aluna já concluiu uma aula dele.")
    @DeleteMapping("/modulos/{id}")
    public ResponseEntity<Void> apagarModulo(@PathVariable Long id) {
        gestao.apagarModulo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- aulas ----

    @Operation(summary = "Criar aula", description = "Entra no fim do módulo. O link do vídeo é validado.")
    @PostMapping("/modulos/{id}/aulas")
    public ResponseEntity<AulaResponse> criarAula(@PathVariable Long id, @Valid @RequestBody AulaGestaoRequest request) {
        return ResponseEntity.status(201).body(gestao.criarAula(id, request));
    }

    @Operation(summary = "Reordenar as aulas do módulo",
            description = "Recebe os ids de todas as aulas na nova ordem. Recusado se alguma aluna já estudou o módulo.")
    @PutMapping("/modulos/{id}/aulas/ordem")
    public ModuloResponse reordenarAulas(@PathVariable Long id, @Valid @RequestBody OrdemRequest request) {
        return gestao.reordenarAulas(id, request.ids());
    }

    @Operation(summary = "Editar aula", description = "Pode ser feito mesmo com alunas estudando (título, duração, XP e link).")
    @PutMapping("/aulas/{id}")
    public AulaResponse atualizarAula(@PathVariable Long id, @Valid @RequestBody AulaGestaoRequest request) {
        return gestao.atualizarAula(id, request);
    }

    @Operation(summary = "Apagar aula", description = "Recusado se alguma aluna já a concluiu.")
    @DeleteMapping("/aulas/{id}")
    public ResponseEntity<Void> apagarAula(@PathVariable Long id) {
        gestao.apagarAula(id);
        return ResponseEntity.noContent().build();
    }
}
