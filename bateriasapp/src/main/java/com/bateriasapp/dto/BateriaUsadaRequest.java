package com.bateriasapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BateriaUsadaRequest(
        @NotNull @Positive Integer pedidoId,
        @Size(max = 200) String descripcion,
        @Size(max = 255) String observaciones) {
}
