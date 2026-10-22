package com.techmentor.api.service;

import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Notificacao;
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

    // Uso interno (conquista, curso concluído, desafio) Não é endpoint
    public void notificar(Usuario usuario, String titulo, String mensagem) {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacaoRepository.save(notificacao);
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
    }
}
