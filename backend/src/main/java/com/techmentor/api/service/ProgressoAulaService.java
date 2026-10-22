package com.techmentor.api.service;

import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.*;
import com.techmentor.api.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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




    @Transactional
    public ProgressoAula concluirAula(String tituloCurso, int ordemModulo, int ordemAula, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Modulo modulo = moduloRepository.findByCursoAndOrdem(curso, ordemModulo)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));

        Aula aula = aulaRepository.findByModuloAndOrdem(modulo, ordemAula)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada!"));

        Matricula matricula = matriculaRepository.findByUsuarioAndCurso(usuario, curso)
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada!"));

        if (progressoAulaRepository.existsByUsuarioAndAula(usuario, aula)) {
            throw new ConflictException("Aula já concluída!");
        }

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
        }

        conquistaService.desbloquear(usuario, "Primeiros Passos");
        desafioService.verificarDesafios(usuario);

        return salvo;
    }

    public Map<String, Long> progressoDoCurso(String tituloCurso, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        Curso curso = cursoService.buscarCursoAtivo(tituloCurso);

        Matricula matricula = matriculaRepository.findByUsuarioAndCurso(usuario, curso)
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada!"));

        Long total = aulaRepository.countByModulo_Curso(curso);

        Long concluidas = progressoAulaRepository.countByUsuarioAndAula_Modulo_Curso(usuario, curso);

        return Map.of("aulasConcluidas", concluidas, "totalAulas", total);
    }

}
