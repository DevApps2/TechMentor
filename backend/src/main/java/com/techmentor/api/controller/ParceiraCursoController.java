package com.techmentor.api.controller;

import com.techmentor.api.dto.AuditoriaDtos.RegistroResponse;
import com.techmentor.api.dto.CursoDtos.AulaResponse;
import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import com.techmentor.api.dto.GestaoCursoDtos.AulaGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.MetricasCursoResponse;
import com.techmentor.api.dto.GestaoCursoDtos.ModuloGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.OrdemRequest;
import com.techmentor.api.dto.ParceiraDtos.ParceiraResponse;
import com.techmentor.api.model.SituacaoCurso;
import com.techmentor.api.service.ParceiraCursoService;
import com.techmentor.api.service.ParceiraService;
import com.techmentor.api.service.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Área da empresa parceira, sob /api/parceira (a SecurityConfig só deixa entrar o papel PARCEIRA).
 * As regras de conteúdo são as mesmas da administradora; a diferença é que aqui só aparecem e só se alteram
 * os cursos da própria parceira, e ela não publica: envia para revisão.
 */
@RestController
@RequestMapping("/api/parceira")
@RequiredArgsConstructor
@Tag(name = "Área da parceira", description = "A empresa parceira monta os próprios cursos e os envia para a revisão da administradora")
public class ParceiraCursoController {

    private final ParceiraCursoService cursos;
    private final ParceiraService parceiras;
    private final UsuarioAtualService usuarios;

    @Operation(summary = "Meu perfil de parceira", description = "Mostra a situação da conta (CONVIDADA, ATIVA ou SUSPENSA) e o motivo, se suspensa.")
    @GetMapping("/perfil")
    public ParceiraResponse perfil() {
        return parceiras.meuPerfil(usuarios.buscarAtual());
    }

    // ---- cursos ----

    @Operation(summary = "Listar meus cursos", description = "Em qualquer situação; filtro opcional.")
    @GetMapping("/cursos")
    public List<CursoResponse> listar(@RequestParam(required = false) SituacaoCurso situacao) {
        return cursos.listar(usuarios.buscarAtual(), situacao);
    }

    @Operation(summary = "Detalhar um dos meus cursos", description = "Inclui o comentário da administradora, se o curso foi devolvido ou recusado.")
    @GetMapping("/cursos/{id}")
    public CursoResponse detalhar(@PathVariable Long id) {
        return cursos.detalhar(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Criar curso", description = "Nasce em RASCUNHO e com o seu nome como instituição. Exige conta verificada.")
    @PostMapping("/cursos")
    public ResponseEntity<CursoResponse> criar(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(201).body(cursos.criar(usuarios.buscarAtual(), request));
    }

    @Operation(summary = "Editar os dados do curso", description = "Não vale para curso em revisão ou arquivado. A instituição é sempre o seu nome.")
    @PutMapping("/cursos/{id}")
    public CursoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return cursos.atualizar(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Enviar para revisão", description = "Só rascunho e só completo (descrição, ao menos um módulo, nenhum módulo vazio). Depois disso a edição fica bloqueada até a resposta.")
    @PostMapping("/cursos/{id}/revisao")
    public CursoResponse enviarParaRevisao(@PathVariable Long id) {
        return cursos.enviarParaRevisao(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Métricas do curso", description = "Matrículas, concluintes e a aula onde mais alunas pararam. Nunca identifica alunas.")
    @GetMapping("/cursos/{id}/metricas")
    public MetricasCursoResponse metricas(@PathVariable Long id) {
        return cursos.metricas(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Histórico do curso", description = "Quem fez o quê e quando, do mais recente para o mais antigo.")
    @GetMapping("/cursos/{id}/historico")
    public List<RegistroResponse> historico(@PathVariable Long id) {
        return cursos.historico(usuarios.buscarAtual(), id);
    }

    // ---- módulos ----

    @Operation(summary = "Criar módulo", description = "Entra no fim do curso.")
    @PostMapping("/cursos/{id}/modulos")
    public ResponseEntity<ModuloResponse> criarModulo(@PathVariable Long id, @Valid @RequestBody ModuloGestaoRequest request) {
        return ResponseEntity.status(201).body(cursos.criarModulo(usuarios.buscarAtual(), id, request));
    }

    @Operation(summary = "Reordenar os módulos", description = "Recusado se alguma aluna já estudou o curso.")
    @PutMapping("/cursos/{id}/modulos/ordem")
    public CursoResponse reordenarModulos(@PathVariable Long id, @Valid @RequestBody OrdemRequest request) {
        return cursos.reordenarModulos(usuarios.buscarAtual(), id, request.ids());
    }

    @Operation(summary = "Editar módulo")
    @PutMapping("/modulos/{id}")
    public ModuloResponse atualizarModulo(@PathVariable Long id, @Valid @RequestBody ModuloGestaoRequest request) {
        return cursos.atualizarModulo(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Apagar módulo", description = "Recusado se alguma aluna já concluiu uma aula dele.")
    @DeleteMapping("/modulos/{id}")
    public ResponseEntity<Void> apagarModulo(@PathVariable Long id) {
        cursos.apagarModulo(usuarios.buscarAtual(), id);
        return ResponseEntity.noContent().build();
    }

    // ---- aulas ----

    @Operation(summary = "Criar aula", description = "Entra no fim do módulo. O link do vídeo é validado.")
    @PostMapping("/modulos/{id}/aulas")
    public ResponseEntity<AulaResponse> criarAula(@PathVariable Long id, @Valid @RequestBody AulaGestaoRequest request) {
        return ResponseEntity.status(201).body(cursos.criarAula(usuarios.buscarAtual(), id, request));
    }

    @Operation(summary = "Reordenar as aulas", description = "Recusado se alguma aluna já estudou o módulo.")
    @PutMapping("/modulos/{id}/aulas/ordem")
    public ModuloResponse reordenarAulas(@PathVariable Long id, @Valid @RequestBody OrdemRequest request) {
        return cursos.reordenarAulas(usuarios.buscarAtual(), id, request.ids());
    }

    @Operation(summary = "Editar aula")
    @PutMapping("/aulas/{id}")
    public AulaResponse atualizarAula(@PathVariable Long id, @Valid @RequestBody AulaGestaoRequest request) {
        return cursos.atualizarAula(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Apagar aula", description = "Recusado se alguma aluna já a concluiu.")
    @DeleteMapping("/aulas/{id}")
    public ResponseEntity<Void> apagarAula(@PathVariable Long id) {
        cursos.apagarAula(usuarios.buscarAtual(), id);
        return ResponseEntity.noContent().build();
    }
}
