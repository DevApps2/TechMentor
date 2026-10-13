package com.techmentor.api.dto;

import com.techmentor.api.model.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

public record UsuarioResponseDTO(
        String nome,
        String email,
        Usuario.Tipo tipo,
        String fotoUrl,
        int xpTotal,
        int nivel,
        int sequenciaDias,
        LocalDate ultimaAtividade
) {
    public static UsuarioResponseDTO from(Usuario u) {
        return new UsuarioResponseDTO(u.getNome(), u.getEmail(), u.getTipo(),
                u.getFotoUrl(), u.getXpTotal(), u.getNivel(), u.getSequenciaDias(),
                u.getUltimaAtividade());
    }
}


