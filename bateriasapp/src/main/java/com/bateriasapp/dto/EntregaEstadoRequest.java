package com.bateriasapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** "instalada" solo aplica al marcar la entrega como entregada. */
public record EntregaEstadoRequest(
        @NotBlank
        @Pattern(regexp = "(?i)(asignada|en_camino|entregada|fallida)", message = "estado desconocido")
        String estado,
        Boolean instalada) {
}
