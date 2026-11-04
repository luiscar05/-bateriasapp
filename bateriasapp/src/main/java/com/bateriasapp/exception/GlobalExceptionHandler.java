package com.bateriasapp.exception;

import com.bateriasapp.dto.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Un solo lugar traduce excepciones a HTTP: servicios y controllers lanzan excepciones de negocio
 * y no se preocupan por códigos de estado. Todas las respuestas de error usan el mismo JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", detalles, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuesta> manejarPeticionMalFormada(Exception ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "Petición mal formada: revisa el JSON, los tipos y los valores permitidos", List.of(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> manejarParametroFaltante(MissingServletRequestParameterException ex,
                                                                   HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio: " + ex.getParameterName(), List.of(), request);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarSolicitudInvalida(SolicitudInvalidaException ex,
                                                                   HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(RecursoNoEncontradoException ex,
                                                              HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), request);
    }

    /** Red de seguridad: los CRUD existentes usan orElseThrow() y así devuelven 404 en vez de 500. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoSuchElement(NoSuchElementException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado", List.of(), request);
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<ErrorRespuesta> manejarConflicto(ConflictoNegocioException ex,
                                                                HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarIntegridad(DataIntegrityViolationException ex,
                                                            HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad (¿hay registros asociados o ya existe?)",
                List.of(), request);
    }

    private ResponseEntity<ErrorRespuesta> construir(HttpStatus status, String mensaje,
                                                     List<String> detalles, HttpServletRequest request) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), mensaje, detalles, request.getRequestURI());
        return ResponseEntity.status(status).body(cuerpo);
    }
}
