package com.bateriasapp.pricing;

import java.math.BigDecimal;

/** Desglose de un precio: total = subtotal + recargo. Todo en pesos enteros. */
public record DesglosePrecio(BigDecimal subtotal, BigDecimal recargo, BigDecimal total) {
}
