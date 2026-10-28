package com.techmentor.api.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

/** Todas as respostas de erro seguem o formato {"erro": "mensagem"}. */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> handle(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(NotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Map<String, String>> conflict(ConflictException ex) {
        return ResponseEntity.status(409).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    ResponseEntity<Map<String, String>> unauthorized(NotAuthorizedException ex) {
        return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException ex) {
        FieldError erro = ex.getBindingResult().getFieldError();
        String mensagem = erro != null && erro.getDefaultMessage() != null
                ? erro.getDefaultMessage() : "Dados inválidos.";
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> unreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Corpo da requisição inválido ou com valor desconhecido."));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<Map<String, String>> missingIdentity(MissingRequestHeaderException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Informe o cabeçalho X-Usuario-Id."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(Map.of("erro", "A operação viola uma regra de integridade ou duplicidade."));
    }
}
