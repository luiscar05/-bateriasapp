package com.bateriasapp.dto;

import com.bateriasapp.model.BateriaUsada;

import java.time.LocalDateTime;

public record BateriaUsadaResponse(
        Integer id,
        Integer pedidoId,
        String descripcion,
        String estadoEvaluacion,
        String destino,
        LocalDateTime fechaRecepcion,
        LocalDateTime fechaEvaluacion,
        String observaciones) {

    public static BateriaUsadaResponse desde(BateriaUsada bateria) {
        return new BateriaUsadaResponse(
                bateria.getId(),
                bateria.getPedido().getId(),
                bateria.getDescripcion(),
                bateria.getEstadoEvaluacion(),
                bateria.getDestino(),
                bateria.getFechaRecepcion(),
                bateria.getFechaEvaluacion(),
                bateria.getObservaciones());
    }
}
