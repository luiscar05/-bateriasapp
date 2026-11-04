package com.bateriasapp.pricing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Regla de precio del negocio, aislada de HTTP y de la BD para poder probarla con tests puros.
 * Por qué BigDecimal: double no representa decimales exactos (0.1 + 0.2 != 0.3) y con dinero
 * eso produce pesos perdidos. El redondeo (HALF_UP, 0 decimales) se aplica una sola vez, al final.
 */
@Service
public class CalculadoraPrecio {

    public static final BigDecimal DESCUENTO_BATERIA_USADA = new BigDecimal("30000");

    public BigDecimal calcularPrecioFinal(BigDecimal precioBase, MetodoPago metodoPago, boolean entregaBateriaUsada) {
        return desglosar(precioBase, metodoPago, entregaBateriaUsada).total();
    }

    public DesglosePrecio desglosar(BigDecimal precioBase, MetodoPago metodoPago, boolean entregaBateriaUsada) {
        Objects.requireNonNull(precioBase, "precioBase es obligatorio");
        Objects.requireNonNull(metodoPago, "metodoPago es obligatorio");

        BigDecimal subtotal = precioBase;
        if (entregaBateriaUsada) {
            subtotal = subtotal.subtract(DESCUENTO_BATERIA_USADA).max(BigDecimal.ZERO);
        }
        BigDecimal recargoExacto = subtotal.multiply(metodoPago.getPorcentajeRecargo());
        BigDecimal total = subtotal.add(recargoExacto).setScale(0, RoundingMode.HALF_UP);
        BigDecimal subtotalEntero = subtotal.setScale(0, RoundingMode.HALF_UP);
        return new DesglosePrecio(subtotalEntero, total.subtract(subtotalEntero), total);
    }

    /**
     * Reconstruye subtotal y recargo a partir del precio final ya pactado en el pedido.
     * Por qué así y no recalculando desde el producto: si el precio base del producto cambió
     * después de la cotización, la factura debe reflejar lo que se pactó, no el precio de hoy.
     * Con recargos de 0%, 6% y 13% el redondeo de ida y vuelta es exacto.
     */
    public DesglosePrecio desglosarPrecioFinal(BigDecimal precioFinal, MetodoPago metodoPago) {
        Objects.requireNonNull(precioFinal, "precioFinal es obligatorio");
        Objects.requireNonNull(metodoPago, "metodoPago es obligatorio");

        BigDecimal total = precioFinal.setScale(0, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(metodoPago.getPorcentajeRecargo());
        BigDecimal subtotal = total.divide(factor, 0, RoundingMode.HALF_UP);
        return new DesglosePrecio(subtotal, total.subtract(subtotal), total);
    }
}
