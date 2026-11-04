package com.bateriasapp.dto;

import java.util.List;

/** El campo "aviso" deja explícito que esto es una sugerencia: una persona confirma la batería. */
public record BateriasSugeridasResponse(
        Integer vehiculoId,
        String marca,
        String modelo,
        Integer anio,
        Integer amperajeRecomendado,
        Integer ccaRecomendado,
        String aviso,
        List<ProductoSugeridoResponse> sugerencias) {
}
