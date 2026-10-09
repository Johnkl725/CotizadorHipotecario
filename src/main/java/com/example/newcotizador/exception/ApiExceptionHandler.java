package com.example.newcotizador.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "com.example.newcotizador.infrastructure.adapter.in.web")
@Slf4j
public class ApiExceptionHandler {
    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, Map<String,String> fields) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(), status.value(), message, fields));
    }
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiErrorResponse> business(BusinessException ex) { return error(ex.getStatus(), ex.getMessage(), Map.of()); }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiErrorResponse> state(IllegalStateException ex) { return error(HttpStatus.CONFLICT, ex.getMessage(), Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String,String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Revisa los datos indicados.", fields);
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<ApiErrorResponse> invalid(RuntimeException ex) { return error(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of()); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> malformed(Exception ex) { return error(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato incorrecto.", Map.of()); }
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiErrorResponse> conflict(Exception ex) {
        return error(HttpStatus.CONFLICT, "Otro usuario modificó los datos o el cliente ya fue registrado. Actualiza la lista y vuelve a intentarlo.", Map.of());
    }
    @ExceptionHandler({TransientDataAccessException.class, DataAccessResourceFailureException.class, TransactionTimedOutException.class, CannotCreateTransactionException.class})
    ResponseEntity<ApiErrorResponse> unavailable(Exception ex) {
        log.warn("Database temporarily unavailable: {}", ex.getClass().getSimpleName());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "El servicio está ocupado. Espera unos segundos y vuelve a intentarlo.", Map.of());
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> denied(Exception ex) { return error(HttpStatus.FORBIDDEN, "No tienes acceso a esta operación.", Map.of()); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception ex) {
        log.error("Unhandled request failure: {}", ex.getClass().getName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "No pudimos completar la operación. Contacta al administrador si el problema continúa.", Map.of());
    }
}
