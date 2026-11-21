package br.univille.odontocare.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todas as respostas de erro têm o mesmo formato:
 * <pre>{ "status": 409, "message": "...", "errors": { "campo": "mensagem" }, "timestamp": "..." }</pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(int status, String message, Map<String, String> errors, OffsetDateTime timestamp) {
        static ResponseEntity<ErrorResponse> of(HttpStatus status, String message, Map<String, String> errors) {
            return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, errors, OffsetDateTime.now()));
        }
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException e) {
        return ErrorResponse.of(e.getStatus(), e.getMessage(), e.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ErrorResponse.of(HttpStatus.BAD_REQUEST, "Verifique os campos", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (e.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String campo = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            if (campo != null) errors.put(campo, "Valor inválido");
        }
        return ErrorResponse.of(HttpStatus.BAD_REQUEST, "Requisição inválida", errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName(), Map.of());
    }

    @ExceptionHandler(java.time.DateTimeException.class)
    ResponseEntity<ErrorResponse> dateTime(java.time.DateTimeException e) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST, "Data ou hora inválida", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return ErrorResponse.of(HttpStatus.CONFLICT, "Operação viola uma regra do banco de dados", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NoResourceFoundException e) {
        return ErrorResponse.of(HttpStatus.NOT_FOUND, "Recurso não encontrado", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> generic(Exception e) {
        log.error("Erro inesperado", e);
        return ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", Map.of());
    }
}
