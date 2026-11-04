package com.bateriasapp.model;

import java.util.Locale;

/** Resultado de la evaluación en bodega de una batería usada. */
public enum EstadoEvaluacion {
    PENDIENTE_EVALUACION, ACEPTADA, RECHAZADA;

    public String getValor() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EstadoEvaluacion desdeValor(String valor) {
        return valueOf(valor.toUpperCase(Locale.ROOT));
    }
}
