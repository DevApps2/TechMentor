package com.techmentor.api.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Todas as respostas de erro seguem o formato de {@link ErroApi}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<ErroApi> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ErroApi.de(status, mensagem));
    }

    /** NotFoundException, ConflictException, BadRequestException... todas herdam de ApiException e trazem o status. */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErroApi> handle(ApiException ex) {
        return resposta(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroApi> invalidBody(MethodArgumentNotValidException ex) {
        // Um campo pode quebrar mais de uma regra; guardamos a primeira mensagem de cada campo
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(),
                    erro.getDefaultMessage() != null ? erro.getDefaultMessage() : "Valor inválido.");
        }
        // "erro" continua sendo uma frase só (a do primeiro campo), para o app antigo não quebrar
        String mensagem = campos.isEmpty() ? "Dados inválidos." : campos.values().iterator().next();
        return ResponseEntity.badRequest().body(ErroApi.deValidacao(mensagem, campos.isEmpty() ? null : campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroApi> unreadableBody(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou com valor desconhecido.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroApi> conflict(DataIntegrityViolationException ex) {
        return resposta(HttpStatus.CONFLICT, "A operação viola uma regra de integridade ou duplicidade.");
    }
}
