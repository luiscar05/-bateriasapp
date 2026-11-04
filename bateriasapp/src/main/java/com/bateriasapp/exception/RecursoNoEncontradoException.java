package com.bateriasapp.exception;

/** Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Integer id) {
        super(recurso + " con id " + id + " no encontrado");
    }
}
