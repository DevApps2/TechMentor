package com.techmentor.api.service;

import com.techmentor.api.model.Usuario;
import com.techmentor.api.exceptions.ForbiddenException;

public final class RegraAcesso {
    private RegraAcesso() { }

    public static void exigirAdmin(Usuario actor) {
        if (actor.getTipo() != Usuario.Tipo.ADMIN) throw new ForbiddenException();
    }

    public static void exigirProprietarioOuAdmin(Usuario actor, Long ownerId) {
        if (!actor.getId().equals(ownerId) && actor.getTipo() != Usuario.Tipo.ADMIN) throw new ForbiddenException();
    }

    public static void exigirMentora(Usuario actor) {
        if (actor.getTipo() != Usuario.Tipo.MENTORA) throw new ForbiddenException();
    }

    public static void exigirAdminOuMentora(Usuario actor) {
        if (actor.getTipo() != Usuario.Tipo.ADMIN && actor.getTipo() != Usuario.Tipo.MENTORA) throw new ForbiddenException();
    }
}
