package com.techmentor.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:900000}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );

        this.expirationMs = expirationMs;
    }

    public String gerarToken(String subject) {

        Date agora = new Date();

        Date expiracao = new Date(
                agora.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(subject)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(secretKey)
                .compact();
    }

    public String extrairSubject(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validarToken(String token, String subjectEsperado) {

        try {
            String subject = extrairSubject(token);

            return subject.equals(subjectEsperado);

        } catch (io.jsonwebtoken.JwtException |
                 IllegalArgumentException e) {

            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}