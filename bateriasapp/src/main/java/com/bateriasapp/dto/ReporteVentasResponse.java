package com.bateriasapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        long totalPedidos,
        BigDecimal totalVentas,
        List<VentasPorMetodoPagoResponse> porMetodoPago) {
}
