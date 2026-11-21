package br.univille.odontocare.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Erro de regra de negócio com status HTTP e (opcionalmente) erros por campo. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errors;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(HttpStatus status, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.errors = errors == null ? Map.of() : errors;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 400 com o erro preso a um campo (o front destaca o campo). */
    public static ApiException campo(String campo, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, Map.of(campo, message));
    }

    /** 409 com o erro preso a um campo (ex.: CPF já cadastrado). */
    public static ApiException conflitoCampo(String campo, String message) {
        return new ApiException(HttpStatus.CONFLICT, message, Map.of(campo, message));
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, String> getErrors() { return errors; }
}
