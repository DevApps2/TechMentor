package com.techmentor.api.service;

import com.techmentor.api.dto.GamificacaoDtos.AulaProgressoResponse;
import com.techmentor.api.dto.GamificacaoDtos.ProgressoAulaResponse;
import com.techmentor.api.dto.GamificacaoDtos.ProgressoCursoResponse;
import com.techmentor.api.dto.GamificacaoDtos.StatusAula;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.*;
import com.techmentor.api.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgressoAulaService {

    private final ProgressoAulaRepository progressoAulaRepository;
    private final MatriculaRepository matriculaRepository;
    private final NotificacaoService notificacaoService;
    private final UsuarioRepository usuarioRepository;
    private final ConquistaService conquistaService;
    private final ModuloRepository moduloRepository;
    private final DesafioService desafioService;
    private final AulaRepository aulaRepository;
    private final CursoService cursoService;
    private final XpService xpService;

    /**
     * Conclui uma aula. As aulas seguem a ordem do curso (a anterior precisa estar concluída) e a
     * matrícula é criada sozinha na primeira aula concluída do curso.
     */
    @Transactional
    public ProgressoAula concluirAula(String tituloCurso, int ordemModulo, int ordemAula, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Modulo modulo = moduloRepository.findByCursoAndOrdem(curso, ordemModulo)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));

        Aula aula = aulaRepository.findByModuloAndOrdem(modulo, ordemAula)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada!"));

        if (progressoAulaRepository.existsByUsuarioAndAula(usuario, aula)) {
            throw new ConflictException("Aula já concluída!");
        }

        exigirAulasAnterioresConcluidas(usuario, curso, aula);

        Matricula matricula = matriculaRepository.findByUsuarioAndCurso(usuario, curso)
                .orElseGet(() -> matricularAutomaticamente(usuario, curso));

        ProgressoAula progresso = new ProgressoAula();
        progresso.setUsuario(usuario);
        progresso.setAula(aula);
        ProgressoAula salvo = progressoAulaRepository.save(progresso);

        xpService.registrar(usuario, aula.getXpRecompensa(), "Aula: " + aula.getTitulo());

        long total = aulaRepository.countByModulo_Curso(curso);
        long concluidas = progressoAulaRepository.countByUsuarioAndAula_Modulo_Curso(usuario, curso);
        if (concluidas == total) {
            matricula.setConcluidoEm(LocalDateTime.now());
            matriculaRepository.save(matricula);
            notificacaoService.notificar(usuario, "Curso concluído!", curso.getTitulo());
            conquistaService.desbloquear(usuario, ConquistaService.CURSO_CONCLUIDO);
        }

        conquistaService.desbloquear(usuario, ConquistaService.PRIMEIRA_AULA);
        if (usuario.getSequenciaDias() >= 3) {
            conquistaService.desbloquear(usuario, ConquistaService.SEQUENCIA_3_DIAS);
        }
        desafioService.verificarDesafios(usuario);

        return salvo;
    }

    @Transactional
    public ProgressoAulaResponse concluirAulaDTO(String tituloCurso, int ordemModulo,
                                                int ordemAula, Long usuarioId) {
        return ProgressoAulaResponse.from(concluirAula(tituloCurso, ordemModulo, ordemAula, usuarioId));
    }

    public Map<String, Long> progressoDoCurso(String tituloCurso, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Long total = aulaRepository.countByModulo_Curso(curso);

        Long concluidas = progressoAulaRepository.countByUsuarioAndAula_Modulo_Curso(usuario, curso);

        return Map.of("aulasConcluidas", concluidas, "totalAulas", total);
    }

    @Transactional
    public ProgressoCursoResponse progressoDoCursoDTO(String tituloCurso, Long usuarioId) {
        Map<String, Long> progresso = progressoDoCurso(tituloCurso, usuarioId);
        return new ProgressoCursoResponse(progresso.get("aulasConcluidas"), progresso.get("totalAulas"));
    }

    /** Aulas do curso na ordem de estudo, cada uma marcada como CONCLUIDA, ATUAL (a próxima) ou BLOQUEADA. */
    @Transactional
    public List<AulaProgressoResponse> listarAulasDoCurso(String tituloCurso, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Map<Long, LocalDateTime> concluidas = new HashMap<>();
        for (ProgressoAula p : progressoAulaRepository.findByUsuarioAndAula_Modulo_Curso(usuario, curso)) {
            concluidas.put(p.getAula().getId(), p.getConcluidaEm());
        }

        List<AulaProgressoResponse> resultado = new ArrayList<>();
        boolean atualEncontrada = false;
        for (Aula aula : aulaRepository.findByModulo_CursoOrderByModulo_OrdemAscOrdemAsc(curso)) {
            StatusAula status;
            if (concluidas.containsKey(aula.getId())) {
                status = StatusAula.CONCLUIDA;
            } else if (!atualEncontrada) {
                status = StatusAula.ATUAL;
                atualEncontrada = true;
            } else {
                status = StatusAula.BLOQUEADA;
            }
            Modulo modulo = aula.getModulo();
            resultado.add(new AulaProgressoResponse(aula.getId(), modulo.getOrdem(), modulo.getTitulo(),
                    aula.getOrdem(), aula.getTitulo(), aula.getDuracaoMinutos(), aula.getXpRecompensa(),
                    status, concluidas.get(aula.getId())));
        }
        return resultado;
    }

    private void exigirAulasAnterioresConcluidas(Usuario usuario, Curso curso, Aula aula) {
        List<Long> concluidas = progressoAulaRepository.findByUsuarioAndAula_Modulo_Curso(usuario, curso).stream()
                .map(p -> p.getAula().getId())
                .toList();

        for (Aula anterior : aulaRepository.findByModulo_CursoOrderByModulo_OrdemAscOrdemAsc(curso)) {
            if (anterior.getId().equals(aula.getId())) {
                return;
            }
            if (!concluidas.contains(anterior.getId())) {
                throw new ConflictException("Conclua as aulas anteriores antes de seguir.");
            }
        }
    }

    private Matricula matricularAutomaticamente(Usuario usuario, Curso curso) {
        Matricula matricula = new Matricula();
        matricula.setUsuario(usuario);
        matricula.setCurso(curso);
        return matriculaRepository.save(matricula);
    }

}
