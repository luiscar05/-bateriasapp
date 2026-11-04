package com.bateriasapp.dto;

import java.math.BigDecimal;

public record VentasPorMetodoPagoResponse(String metodoPago, long cantidadPedidos, BigDecimal totalVentas) {
}
