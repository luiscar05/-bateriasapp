package com.bateriasapp.dto;

import com.bateriasapp.model.Factura;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record FacturaResponse(
        Integer id,
        String numeroFactura,
        LocalDateTime fecha,
        Integer pedidoId,
        String clienteNombre,
        String metodoPago,
        BigDecimal subtotal,
        BigDecimal recargo,
        BigDecimal total) {

    public static FacturaResponse desde(Factura factura) {
        return new FacturaResponse(
                factura.getId(),
                factura.getNumeroFactura(),
                factura.getFecha(),
                factura.getPedido().getId(),
                factura.getPedido().getCliente().getNombre(),
                factura.getPedido().getMetodoPago(),
                factura.getSubtotal().setScale(0, RoundingMode.HALF_UP),
                factura.getRecargo().setScale(0, RoundingMode.HALF_UP),
                factura.getTotal().setScale(0, RoundingMode.HALF_UP));
    }
}
