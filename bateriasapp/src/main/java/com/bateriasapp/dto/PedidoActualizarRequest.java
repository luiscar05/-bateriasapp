package com.bateriasapp.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Todos los campos son opcionales: null significa "no cambiar". */
public record PedidoActualizarRequest(
        @Positive Integer productoId,
        @Pattern(regexp = "(?i)(efectivo|transferencia|tarjeta|addi)",
                message = "debe ser efectivo, transferencia, tarjeta o addi")
        String metodoPago,
        Boolean entregaBateriaUsada) {
}
