package com.bateriasapp.model;

import java.util.Locale;

/**
 * Máquina de estados del pedido. Por qué un enum y no Strings sueltos: las transiciones válidas
 * viven en UN solo lugar, el compilador avisa si falta un caso (switch exhaustivo) y no hay typos.
 * En la BD se guarda el nombre en minúscula (coincide con el CHECK de la tabla pedidos).
 */
public enum EstadoPedido {
    COTIZADO, CONFIRMADO, PAGADO, EN_ENTREGA, ENTREGADO, FACTURADO, CANCELADO;

    public String getValor() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EstadoPedido desdeValor(String valor) {
        return valueOf(valor.toUpperCase(Locale.ROOT));
    }

    /** Avanza un paso en el flujo; CANCELADO solo es posible antes de ENTREGADO. */
    public boolean puedePasarA(EstadoPedido destino) {
        return switch (this) {
            case COTIZADO -> destino == CONFIRMADO || destino == CANCELADO;
            case CONFIRMADO -> destino == PAGADO || destino == CANCELADO;
            case PAGADO -> destino == EN_ENTREGA || destino == CANCELADO;
            case EN_ENTREGA -> destino == ENTREGADO || destino == CANCELADO;
            case ENTREGADO -> destino == FACTURADO;
            case FACTURADO, CANCELADO -> false;
        };
    }

    /** ENTREGADO, FACTURADO y CANCELADO ya no admiten cambios de datos. */
    public boolean esModificable() {
        return this != ENTREGADO && this != FACTURADO && this != CANCELADO;
    }
}
