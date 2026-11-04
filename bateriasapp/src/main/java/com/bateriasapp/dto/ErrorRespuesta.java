package com.bateriasapp.dto;

import java.time.LocalDateTime;
import java.util.List;

/** JSON de error uniforme para toda la API. */
public record ErrorRespuesta(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        List<String> detalles,
        String ruta) {
}
