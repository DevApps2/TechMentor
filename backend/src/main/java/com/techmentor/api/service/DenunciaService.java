package com.techmentor.api.service;

import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.dto.SegurancaDtos.DenunciaRequest;
import com.techmentor.api.dto.SegurancaDtos.DenunciaResponse;
import com.techmentor.api.repository.DenunciaRepository;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class DenunciaService {
    private final DenunciaRepository denuncias;
    private final UsuarioRepository usuarios;

    public DenunciaService(DenunciaRepository denuncias, UsuarioRepository usuarios) {
        this.denuncias = denuncias;
        this.usuarios = usuarios;
    }

    public DenunciaResponse criar(Usuario actor, DenunciaRequest request) {
        if (request == null || request.motivo() == null)
            throw ApiException.badRequest("Motivo da denúncia é obrigatório.");
        Usuario alvo = request.usuarioDenunciadoId() == null ? null : buscarUsuario(request.usuarioDenunciadoId());
        if (alvo != null && alvo.getId().equals(actor.getId()))
            throw ApiException.badRequest("Não é possível denunciar a si mesmo.");
        Denuncia denuncia = new Denuncia();
        denuncia.setDenunciante(actor);
        denuncia.setDenunciado(alvo);
        denuncia.setMotivo(request.motivo());
        denuncia.setDescricao(request.descricao());
        return toResponse(denuncias.save(denuncia));
    }

    @Transactional(readOnly = true)
    public List<DenunciaResponse> minhas(Usuario actor) {
        return denuncias.findByDenuncianteIdOrderByCriadaEmDesc(actor.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DenunciaResponse> listarTodas(Usuario actor) {
        RegraAcesso.exigirAdmin(actor);
        return denuncias.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DenunciaResponse obter(Usuario actor, Long id) {
        Denuncia denuncia = denuncias.findById(id).orElseThrow(() -> ApiException.notFound("Denúncia não encontrada."));
        if (actor.getTipo() != Usuario.Tipo.ADMIN && !denuncia.getDenunciante().getId().equals(actor.getId()))
            throw ApiException.forbidden();
        return toResponse(denuncia);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    }

    private DenunciaResponse toResponse(Denuncia denuncia) {
        return new DenunciaResponse(denuncia.getId(), denuncia.getDenunciante().getId(),
                denuncia.getDenunciado() == null ? null : denuncia.getDenunciado().getId(), denuncia.getMotivo(),
                denuncia.getDescricao(), denuncia.getStatus(), denuncia.getCriadaEm());
    }
}
