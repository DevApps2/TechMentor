package com.techmentor.api.service;

import com.techmentor.api.dto.MentoriaDtos.*;
import com.techmentor.api.repository.*;
import com.techmentor.api.model.*;
import com.techmentor.api.ApiException;
import com.techmentor.api.service.RegraAcesso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MentoraService {
    private final PerfilMentoraRepository mentoras;
    private final AreaAtuacaoRepository areas;
    private final HorarioDisponivelRepository horarios;
    private final MentoriaRepository mentorias;

    public MentoraService(PerfilMentoraRepository mentoras, AreaAtuacaoRepository areas,
                          HorarioDisponivelRepository horarios, MentoriaRepository mentorias) {
        this.mentoras = mentoras;
        this.areas = areas;
        this.horarios = horarios;
        this.mentorias = mentorias;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> listarAreas() {
        return areas.findAll().stream().sorted(Comparator.comparing(AreaAtuacao::getNome))
                .map(area -> new AreaResponse(area.getId(), area.getNome())).toList();
    }

    public AreaResponse criarArea(Usuario actor, String nome) {
        RegraAcesso.exigirAdmin(actor);
        if (nome == null || nome.isBlank() || nome.trim().length() > 80)
            throw ApiException.badRequest("Nome da área deve ter entre 1 e 80 caracteres.");
        if (areas.findAll().stream().anyMatch(area -> area.getNome().equalsIgnoreCase(nome.trim())))
            throw ApiException.conflict("Área já cadastrada.");
        AreaAtuacao area = new AreaAtuacao();
        area.setNome(nome.trim());
        area = areas.save(area);
        return new AreaResponse(area.getId(), area.getNome());
    }

    @Transactional(readOnly = true)
    public List<MentoraResponse> listarMentoras() {
        return mentoras.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MentoraResponse obterMentora(Long id) { return toResponse(buscarMentora(id)); }

    public MentoraResponse criarMentora(Usuario actor, MentoraRequest request) {
        validarPerfil(request);
        if (mentoras.findAll().stream().anyMatch(m -> m.getUsuario().getId().equals(actor.getId())))
            throw ApiException.conflict("Este usuário já possui perfil de mentora.");
        PerfilMentora mentora = new PerfilMentora();
        mentora.setUsuario(actor);
        aplicarDados(mentora, request);
        return toResponse(mentoras.save(mentora));
    }

    public MentoraResponse atualizarMentora(Usuario actor, Long id, MentoraRequest request) {
        validarPerfil(request);
        PerfilMentora mentora = buscarMentora(id);
        RegraAcesso.exigirProprietarioOuAdmin(actor, mentora.getUsuario().getId());
        aplicarDados(mentora, request);
        return toResponse(mentora);
    }

    public void excluirMentora(Usuario actor, Long id) {
        PerfilMentora mentora = buscarMentora(id);
        RegraAcesso.exigirProprietarioOuAdmin(actor, mentora.getUsuario().getId());
        if (mentorias.existsByHorarioMentoraId(id))
            throw ApiException.conflict("Não é possível excluir mentora com agendamentos registrados.");
        mentoras.delete(mentora);
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listarHorarios(Long mentoraId) {
        buscarMentora(mentoraId);
        return horarios.findByMentoraIdAndReservadoFalseOrderByInicio(mentoraId).stream()
                .map(this::toHorarioResponse).toList();
    }

    public HorarioResponse criarHorario(Usuario actor, Long mentoraId, HorarioRequest request) {
        PerfilMentora mentora = buscarMentora(mentoraId);
        RegraAcesso.exigirProprietarioOuAdmin(actor, mentora.getUsuario().getId());
        if (request == null || request.inicio() == null || request.fim() == null
                || !request.inicio().isAfter(LocalDateTime.now()) || !request.fim().isAfter(request.inicio()))
            throw ApiException.badRequest("Informe início futuro e fim posterior ao início.");
        boolean sobreposto = horarios.findByMentoraId(mentoraId).stream()
                .anyMatch(h -> request.inicio().isBefore(h.getFim()) && request.fim().isAfter(h.getInicio()));
        if (sobreposto) throw ApiException.conflict("O horário sobrepõe outra disponibilidade.");
        HorarioDisponivel horario = new HorarioDisponivel();
        horario.setMentora(mentora);
        horario.setInicio(request.inicio());
        horario.setFim(request.fim());
        return toHorarioResponse(horarios.save(horario));
    }

    public void excluirHorario(Usuario actor, Long id) {
        HorarioDisponivel horario = horarios.findById(id)
                .orElseThrow(() -> ApiException.notFound("Horário não encontrado."));
        RegraAcesso.exigirProprietarioOuAdmin(actor, horario.getMentora().getUsuario().getId());
        if (horario.isReservado()) throw ApiException.conflict("Horário reservado não pode ser excluído.");
        horarios.delete(horario);
    }

    private PerfilMentora buscarMentora(Long id) {
        return mentoras.findById(id).orElseThrow(() -> ApiException.notFound("Mentora não encontrada."));
    }

    private void validarPerfil(MentoraRequest request) {
        if (request == null) throw ApiException.badRequest("Dados do perfil são obrigatórios.");
        if (request.anosExperiencia() != null && request.anosExperiencia() < 0)
            throw ApiException.badRequest("Anos de experiência não pode ser negativo.");
    }

    private void aplicarDados(PerfilMentora mentora, MentoraRequest request) {
        if (request.bio() != null && request.bio().length() > 2000)
            throw ApiException.badRequest("Bio excede 2000 caracteres.");
        mentora.setBio(request.bio());
        mentora.setCargo(request.cargo());
        mentora.setEmpresa(request.empresa());
        if (request.anosExperiencia() != null) mentora.setAnosExperiencia(request.anosExperiencia());
        if (request.areasIds() != null) {
            Set<AreaAtuacao> selecionadas = new HashSet<>(areas.findAllById(request.areasIds()));
            if (selecionadas.size() != request.areasIds().size())
                throw ApiException.badRequest("Uma ou mais áreas de atuação não existem.");
            mentora.setAreas(selecionadas);
        }
    }

    private MentoraResponse toResponse(PerfilMentora mentora) {
        Set<String> nomesAreas = mentora.getAreas().stream().map(AreaAtuacao::getNome)
                .collect(Collectors.toCollection(TreeSet::new));
        return new MentoraResponse(mentora.getId(), mentora.getUsuario().getId(), mentora.getUsuario().getNome(),
                mentora.getUsuario().getFotoUrl(), mentora.getBio(), mentora.getCargo(), mentora.getEmpresa(),
                mentora.getAnosExperiencia(), mentora.getNotaMedia(), mentora.getTotalAvaliacoes(), nomesAreas);
    }

    private HorarioResponse toHorarioResponse(HorarioDisponivel horario) {
        return new HorarioResponse(horario.getId(), horario.getMentora().getId(), horario.getInicio(),
                horario.getFim(), horario.isReservado());
    }
}
