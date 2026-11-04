package com.bateriasapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Resultado de la evaluación en bodega. Si es "aceptada" el destino es obligatorio;
 * si es "rechazada" no debe enviarse destino (esa coherencia la valida el servicio).
 */
public record BateriaUsadaEvaluacionRequest(
        @NotBlank
        @Pattern(regexp = "(?i)(aceptada|rechazada)", message = "debe ser aceptada o rechazada")
        String estadoEvaluacion,
        @Pattern(regexp = "(?i)(lote_mensual|reventa)", message = "debe ser lote_mensual o reventa")
        String destino,
        @Size(max = 255) String observaciones) {
}
