package com.bateriasapp.pricing;

import java.math.BigDecimal;

/**
 * Método de pago y su recargo sobre el subtotal.
 * El porcentaje es BigDecimal (fracción: 0.13 = 13%) para no arrastrar errores de punto flotante.
 */
public enum MetodoPago {
    EFECTIVO("0"),
    TRANSFERENCIA("0"),
    TARJETA("0.06"),
    ADDI("0.13");

    private final BigDecimal porcentajeRecargo;

    MetodoPago(String porcentajeRecargo) {
        this.porcentajeRecargo = new BigDecimal(porcentajeRecargo);
    }

    public BigDecimal getPorcentajeRecargo() {
        return porcentajeRecargo;
    }
}
