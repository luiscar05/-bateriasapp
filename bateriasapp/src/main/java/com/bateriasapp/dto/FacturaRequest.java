package com.bateriasapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FacturaRequest(@NotNull @Positive Integer pedidoId) {
}
