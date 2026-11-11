package com.techmentor.api.service;

import com.techmentor.api.model.BloqueioUsuario;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.dto.SegurancaDtos.BloqueioResponse;
import com.techmentor.api.repository.BloqueioUsuarioRepository;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
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
        if (usuarioId == null) throw new BadRequestException("Usuário que será bloqueado é obrigatório.");
        Usuario alvo = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        if (alvo.getId().equals(actor.getId())) throw new BadRequestException("Não é possível bloquear a si mesmo.");
        if (bloqueios.existsByBloqueadorIdAndBloqueadoId(actor.getId(), alvo.getId()))
            throw new ConflictException("Usuário já está bloqueado.");
        BloqueioUsuario bloqueio = new BloqueioUsuario();
        bloqueio.setBloqueador(actor);
        bloqueio.setBloqueado(alvo);
        bloqueios.save(bloqueio);
        return new BloqueioResponse(alvo.getId(), alvo.getNome(), bloqueio.getCriadoEm());
    }

    public void desbloquear(Usuario actor, Long usuarioId) {
        BloqueioUsuario bloqueio = bloqueios.findByBloqueadorIdAndBloqueadoId(actor.getId(), usuarioId)
                .orElseThrow(() -> new NotFoundException("Bloqueio não encontrado."));
        bloqueios.delete(bloqueio);
    }
}
