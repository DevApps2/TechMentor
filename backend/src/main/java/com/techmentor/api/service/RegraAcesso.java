package com.techmentor.api.service;

import com.techmentor.api.model.Usuario;
import com.techmentor.api.exceptions.ApiException;

public final class RegraAcesso {
    private RegraAcesso() { }

    public static void exigirAdmin(Usuario actor) {
        if (actor.getTipo() != Usuario.Tipo.ADMIN) throw ApiException.forbidden();
    }

    public static void exigirProprietarioOuAdmin(Usuario actor, Long ownerId) {
        if (!actor.getId().equals(ownerId) && actor.getTipo() != Usuario.Tipo.ADMIN) throw ApiException.forbidden();
    }
}
