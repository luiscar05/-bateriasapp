package com.bateriasapp.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Todos los campos son opcionales: null significa "no cambiar". */
public record EntregaActualizarRequest(
        @Positive Integer domiciliarioId,
        @Size(max = 200) String direccion,
        LocalDateTime fechaEntrega) {
}
