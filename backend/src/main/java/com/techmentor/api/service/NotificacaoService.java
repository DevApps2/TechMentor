package com.techmentor.api.service;

import com.techmentor.api.dto.GamificacaoDtos.NotificacaoResponse;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.DestinoNotificacao;
import com.techmentor.api.model.Notificacao;
import com.techmentor.api.model.TipoNotificacao;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.NotificacaoRepository;
import com.techmentor.api.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Notificacao> listar(Long usuarioId, Boolean lida) {
        Usuario usuario = buscarUsuario(usuarioId);
        return lida == null
                ? notificacaoRepository.findByUsuarioOrderByCriadaEmDesc(usuario)
                : notificacaoRepository.findByUsuarioAndLidaOrderByCriadaEmDesc(usuario, lida);
    }

    @Transactional
    public List<NotificacaoResponse> listarDTO(Long usuarioId, Boolean lida) {
        return listar(usuarioId, lida).stream().map(NotificacaoResponse::from).toList();
    }

    // id nulo = marca todas as não lidas; com id = marca só aquela
    @Transactional
    public void marcarComoLida(Long usuarioId, Long id) {
        Usuario usuario = buscarUsuario(usuarioId);
        List<Notificacao> alvo = (id == null)
                ? notificacaoRepository.findByUsuarioAndLidaOrderByCriadaEmDesc(usuario, false)
                : List.of(notificacaoRepository.findByIdAndUsuario(id, usuario)
                          .orElseThrow(() -> new NotFoundException("Notificação não encontrada!")));
        alvo.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(alvo);
    }

    // Uso interno. Não é endpoint. Sem tipo nem destino, é uma notificação geral que só se marca como lida
    public void notificar(Usuario usuario, String titulo, String mensagem) {
        notificar(usuario, TipoNotificacao.GERAL, DestinoNotificacao.NENHUM, null, titulo, mensagem);
    }

    /** referencia completa o destino (ex.: o título do curso para CURSO); pode ser nula. */
    public void notificar(Usuario usuario, TipoNotificacao tipo, DestinoNotificacao destino, String referencia,
                          String titulo, String mensagem) {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);
        notificacao.setTipo(tipo);
        notificacao.setDestino(destino);
        notificacao.setReferencia(referencia);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacaoRepository.save(notificacao);
    }

    /** Avisa todas as administradoras ativas. */
    public void notificarAdministradoras(TipoNotificacao tipo, DestinoNotificacao destino, String referencia,
                                         String titulo, String mensagem) {
        usuarioRepository.findByTipoAndAtivoTrue(Usuario.Tipo.ADMIN)
                .forEach(admin -> notificar(admin, tipo, destino, referencia, titulo, mensagem));
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
    }
}
