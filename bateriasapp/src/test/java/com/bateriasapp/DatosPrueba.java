package com.bateriasapp;

import com.bateriasapp.model.Cliente;
import com.bateriasapp.model.Domiciliario;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fábrica de objetos de prueba para no repetir setters en cada test. */
public final class DatosPrueba {

    private DatosPrueba() {
    }

    public static Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setNombre("Ana Gómez");
        cliente.setDireccion("Calle 100 # 15-20");
        return cliente;
    }

    public static Domiciliario domiciliario(int id) {
        Domiciliario domiciliario = new Domiciliario();
        domiciliario.setId(id);
        domiciliario.setNombre("Carlos Ruiz");
        return domiciliario;
    }

    public static Producto producto(int id, String precioBase, boolean disponible) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Batería " + id);
        producto.setTipo("bateria");
        producto.setAmperaje(60);
        producto.setCca(540);
        producto.setPrecioBase(new BigDecimal(precioBase));
        producto.setCondicion("nueva");
        producto.setDisponible(disponible);
        return producto;
    }

    /** Pedido ya guardado: el precioFinal se pasa aparte para poder simular precios pactados antes de un cambio. */
    public static Pedido pedido(int id, String estado, String metodoPago, boolean bateriaUsada,
                                String precioBaseProducto, String precioFinal) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setCliente(cliente());
        pedido.setProducto(producto(10, precioBaseProducto, true));
        pedido.setMetodoPago(metodoPago);
        pedido.setEntregaBateriaUsada(bateriaUsada);
        pedido.setPrecioFinal(new BigDecimal(precioFinal));
        pedido.setEstado(estado);
        pedido.setFechaCreacion(LocalDateTime.of(2026, 10, 20, 9, 30));
        return pedido;
    }
}
