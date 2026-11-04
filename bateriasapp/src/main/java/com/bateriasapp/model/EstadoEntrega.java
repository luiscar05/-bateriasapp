package com.bateriasapp.model;

import java.util.Locale;

/** Estados de una entrega; en la BD se guardan en minúscula (CHECK de la tabla entregas). */
public enum EstadoEntrega {
    ASIGNADA, EN_CAMINO, ENTREGADA, FALLIDA;

    public String getValor() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EstadoEntrega desdeValor(String valor) {
        return valueOf(valor.toUpperCase(Locale.ROOT));
    }

    public boolean puedePasarA(EstadoEntrega destino) {
        return switch (this) {
            case ASIGNADA -> destino == EN_CAMINO || destino == FALLIDA;
            case EN_CAMINO -> destino == ENTREGADA || destino == FALLIDA;
            case ENTREGADA, FALLIDA -> false;
        };
    }

    /** Domiciliario, dirección y hora solo se corrigen mientras la entrega sigue en curso. */
    public boolean esModificable() {
        return this == ASIGNADA || this == EN_CAMINO;
    }
}
