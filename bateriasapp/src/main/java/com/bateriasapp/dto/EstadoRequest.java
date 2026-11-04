package com.bateriasapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstadoRequest(
        @NotBlank
        @Pattern(regexp = "(?i)(cotizado|confirmado|pagado|en_entrega|entregado|facturado|cancelado)",
                message = "estado desconocido")
        String estado) {
}
