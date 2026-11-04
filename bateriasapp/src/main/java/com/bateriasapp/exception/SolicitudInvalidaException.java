package com.bateriasapp.exception;

/** Regla de validación que Bean Validation no puede expresar (campos que dependen entre sí). HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
