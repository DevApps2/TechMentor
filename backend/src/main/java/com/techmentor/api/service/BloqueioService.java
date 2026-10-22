package com.techmentor.api.service;

import com.techmentor.api.model.BloqueioUsuario;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.dto.SegurancaDtos.BloqueioResponse;
import com.techmentor.api.repository.BloqueioUsuarioRepository;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class BloqueioService {
    private final BloqueioUsuarioRepository bloqueios;
    private final UsuarioRepository usuarios;

    public BloqueioService(BloqueioUsuarioRepository bloqueios, UsuarioRepository usuarios) {
        this.bloqueios = bloqueios;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<BloqueioResponse> listar(Usuario actor) {
        return bloqueios.findByBloqueadorIdOrderByCriadoEmDesc(actor.getId()).stream()
                .map(b -> new BloqueioResponse(b.getBloqueado().getId(), b.getBloqueado().getNome(), b.getCriadoEm()))
                .toList();
    }

    public BloqueioResponse bloquear(Usuario actor, Long usuarioId) {
        if (usuarioId == null) throw ApiException.badRequest("Usuário que será bloqueado é obrigatório.");
        Usuario alvo = usuarios.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        if (alvo.getId().equals(actor.getId())) throw ApiException.badRequest("Não é possível bloquear a si mesmo.");
        if (bloqueios.existsByBloqueadorIdAndBloqueadoId(actor.getId(), alvo.getId()))
            throw ApiException.conflict("Usuário já está bloqueado.");
        BloqueioUsuario bloqueio = new BloqueioUsuario();
        bloqueio.setBloqueador(actor);
        bloqueio.setBloqueado(alvo);
        bloqueios.save(bloqueio);
        return new BloqueioResponse(alvo.getId(), alvo.getNome(), bloqueio.getCriadoEm());
    }

    public void desbloquear(Usuario actor, Long usuarioId) {
        BloqueioUsuario bloqueio = bloqueios.findByBloqueadorIdAndBloqueadoId(actor.getId(), usuarioId)
                .orElseThrow(() -> ApiException.notFound("Bloqueio não encontrado."));
        bloqueios.delete(bloqueio);
    }
}
