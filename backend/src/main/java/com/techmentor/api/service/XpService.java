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

import java.util.List;

@Service
@RequiredArgsConstructor
public class XpService {

    private final HistoricoXpRepository historicoXpRepository;
    private final UsuarioRepository usuarioRepository;

    public void registrar(Usuario usuario, int pontos, String motivo) {
        usuario.setXpTotal(usuario.getXpTotal() + pontos);
        usuarioRepository.save(usuario);

        HistoricoXp historico = new HistoricoXp();
        historico.setUsuario(usuario);
        historico.setPontos(pontos);
        historico.setMotivo(motivo);
        historicoXpRepository.save(historico);
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
