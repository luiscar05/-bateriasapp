package com.bateriasapp.dto;

import com.bateriasapp.model.Entrega;

import java.time.LocalDateTime;

public record EntregaResponse(
        Integer id,
        Integer pedidoId,
        Integer domiciliarioId,
        String domiciliarioNombre,
        String direccion,
        LocalDateTime fechaEntrega,
        String estado,
        Boolean instalada) {

    public static EntregaResponse desde(Entrega entrega) {
        return new EntregaResponse(
                entrega.getId(),
                entrega.getPedido().getId(),
                entrega.getDomiciliario().getId(),
                entrega.getDomiciliario().getNombre(),
                entrega.getDireccion(),
                entrega.getFechaEntrega(),
                entrega.getEstado(),
                entrega.getInstalada());
    }
}
