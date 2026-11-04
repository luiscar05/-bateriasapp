package com.bateriasapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Nótese que NO hay precioFinal: el precio siempre se calcula en el servidor.
 * Es un record (Java 21): inmutable y sin getters/setters que escribir, sin necesitar Lombok.
 */
public record PedidoRequest(
        @NotNull @Positive Integer clienteId,
        @NotNull @Positive Integer productoId,
        @NotBlank
        @Pattern(regexp = "(?i)(efectivo|transferencia|tarjeta|addi)",
                message = "debe ser efectivo, transferencia, tarjeta o addi")
        String metodoPago,
        @NotNull Boolean entregaBateriaUsada) {
}
