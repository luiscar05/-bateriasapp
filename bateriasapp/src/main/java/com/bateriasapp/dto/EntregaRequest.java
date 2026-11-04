package com.bateriasapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Si no se envía dirección, se usa la dirección registrada del cliente. */
public record EntregaRequest(
        @NotNull @Positive Integer pedidoId,
        @NotNull @Positive Integer domiciliarioId,
        @Size(max = 200) String direccion,
        @NotNull LocalDateTime fechaEntrega) {
}
