package com.techmentor.api.service;

import com.techmentor.api.dto.AuditoriaDtos.RegistroResponse;
import com.techmentor.api.model.AcaoAuditada;
import com.techmentor.api.model.RegistroAuditoria;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.RegistroAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * O histórico de auditoria: grava quem fez o quê e devolve a lista de um curso ou de uma parceira.
 * Grava na mesma transação da ação: se a ação desfaz, o registro dela também.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int LIMITE_DO_DETALHE = 500;

    private final RegistroAuditoriaRepository registros;

    /** Registra em nome de quem está logada (o token). Sem ninguém logada, fica como ação do sistema. */
    @Transactional
    public void registrar(AcaoAuditada acao, String entidade, Long entidadeId, String detalhe) {
        registrar(autorAtual(), acao, entidade, entidadeId, detalhe);
    }

    /** Para quem ainda não tem token (a parceira que aceita o convite). */
    @Transactional
    public void registrar(Usuario autor, AcaoAuditada acao, String entidade, Long entidadeId, String detalhe) {
        RegistroAuditoria registro = new RegistroAuditoria();
        if (autor != null) {
            registro.setAutorId(autor.getId());
            registro.setAutorNome(autor.getNome());
            registro.setAutorPapel(autor.getTipo().name());
        }
        registro.setAcao(acao);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setDetalhe(detalhe == null ? null : cortar(detalhe));
        registros.save(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroResponse> historico(String entidade, Long entidadeId) {
        return registros.findByEntidadeAndEntidadeIdOrderByQuandoDescIdDesc(entidade, entidadeId).stream()
                .map(RegistroResponse::from)
                .toList();
    }

    private static Usuario autorAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.isAuthenticated() && autenticacao.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }

    private static String cortar(String texto) {
        return texto.length() <= LIMITE_DO_DETALHE ? texto : texto.substring(0, LIMITE_DO_DETALHE - 1) + "…";
    }
}
