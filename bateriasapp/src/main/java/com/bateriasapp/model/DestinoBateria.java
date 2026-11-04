package com.bateriasapp.model;

import java.util.Locale;

/** A dónde va una batería usada aceptada: venta en lote mensual o reventa como usada. */
public enum DestinoBateria {
    LOTE_MENSUAL, REVENTA;

    public String getValor() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DestinoBateria desdeValor(String valor) {
        return valueOf(valor.toUpperCase(Locale.ROOT));
    }
}
