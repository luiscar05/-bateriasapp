package com.bateriasapp.exception;

/** La operación choca con el estado actual del recurso o con una regla de negocio (HTTP 409). */
public class ConflictoNegocioException extends RuntimeException {

    public ConflictoNegocioException(String mensaje) {
        super(mensaje);
    }
}
