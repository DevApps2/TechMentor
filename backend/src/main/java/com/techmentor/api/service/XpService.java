package com.techmentor.api.service;

import com.techmentor.api.dto.GamificacaoDtos.HistoricoXpResponse;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.HistoricoXp;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.HistoricoXpRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class XpService {

    private final HistoricoXpRepository historicoXpRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;

    public void registrar(Usuario usuario, int pontos, String motivo) {
        int nivelAnterior = NivelRegraService.nivelPara(usuario.getXpTotal());

        usuario.setXpTotal(usuario.getXpTotal() + pontos);
        int nivelAtual = NivelRegraService.nivelPara(usuario.getXpTotal());
        usuario.setNivel(nivelAtual);
        atualizarSequencia(usuario, LocalDate.now());
        usuarioRepository.save(usuario);

        HistoricoXp historico = new HistoricoXp();
        historico.setUsuario(usuario);
        historico.setPontos(pontos);
        historico.setMotivo(motivo);
        historicoXpRepository.save(historico);

        if (nivelAtual > nivelAnterior) {
            notificacaoService.notificar(usuario, "Você subiu de nível!",
                    "Agora você é nível " + nivelAtual + " · " + NivelRegraService.titulo(nivelAtual));
        }
    }

    /** Ganhar XP conta como atividade do dia: dias seguidos aumentam a sequência, uma pausa a reinicia. */
    void atualizarSequencia(Usuario usuario, LocalDate hoje) {
        LocalDate ultima = usuario.getUltimaAtividade();
        if (hoje.equals(ultima)) {
            return;
        }
        boolean diaSeguinte = ultima != null && ultima.plusDays(1).equals(hoje);
        usuario.setSequenciaDias(diaSeguinte ? usuario.getSequenciaDias() + 1 : 1);
        usuario.setUltimaAtividade(hoje);
    }

    public List<HistoricoXp> listar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        return historicoXpRepository.findByUsuarioOrderByCriadoEmDesc(usuario);
    }

    @Transactional(readOnly = true)
    public List<HistoricoXpResponse> listarDTO(Long usuarioId) {
        return listar(usuarioId).stream().map(HistoricoXpResponse::from).toList();
    }
}
