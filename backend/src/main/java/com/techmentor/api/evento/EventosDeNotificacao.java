package com.techmentor.api.evento;

import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.Usuario;

/** Fatos do sistema que geram uma notificação para alguém. Quem publica não sabe quem escuta. */
public final class EventosDeNotificacao {
    private EventosDeNotificacao() { }

    public record ConquistaDesbloqueada(Usuario usuario, String nomeDaConquista) { }

    public record NivelSubiu(Usuario usuario, int nivel) { }

    public record CursoConcluido(Usuario usuario, String tituloDoCurso) { }

    public record DesafioConcluido(Usuario usuario, String tituloDoDesafio) { }

    // nomeDoDenunciado é nulo em denúncias gerais (sem pessoa indicada)
    public record StatusDaDenunciaMudou(Usuario denunciante, String nomeDoDenunciado, Denuncia.Status novoStatus) { }
}