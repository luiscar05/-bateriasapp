package com.bateriasapp.dto;

import com.bateriasapp.model.Producto;

import java.math.BigDecimal;

public record ProductoSugeridoResponse(
        Integer id,
        String nombre,
        int amperaje,
        int cca,
        String tipoCaja,
        String polaridad,
        BigDecimal precioBase,
        String condicion) {

    public static ProductoSugeridoResponse desde(Producto producto) {
        return new ProductoSugeridoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getAmperaje(),
                producto.getCca(),
                producto.getTipoCaja(),
                producto.getPolaridad(),
                producto.getPrecioBase(),
                producto.getCondicion());
    }
}
