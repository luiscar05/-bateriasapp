package com.bateriasapp.dto;

import com.bateriasapp.model.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Por qué un DTO de salida: la API no queda atada a la estructura de la tabla
 * y no se filtran datos de las entidades relacionadas (cédula, dirección del cliente, etc.).
 */
public record PedidoResponse(
        Integer id,
        Integer clienteId,
        String clienteNombre,
        Integer productoId,
        String productoNombre,
        String metodoPago,
        Boolean entregaBateriaUsada,
        BigDecimal precioFinal,
        String estado,
        LocalDateTime fechaCreacion) {

    public static PedidoResponse desde(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getCliente().getNombre(),
                pedido.getProducto().getId(),
                pedido.getProducto().getNombre(),
                pedido.getMetodoPago(),
                pedido.getEntregaBateriaUsada(),
                // La BD guarda decimal(10,2); el negocio trabaja en pesos enteros.
                pedido.getPrecioFinal().setScale(0, RoundingMode.HALF_UP),
                pedido.getEstado(),
                pedido.getFechaCreacion());
    }
}
