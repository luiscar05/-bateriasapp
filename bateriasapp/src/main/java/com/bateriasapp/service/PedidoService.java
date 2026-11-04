package com.bateriasapp.service;

import com.bateriasapp.dto.EstadoRequest;
import com.bateriasapp.dto.PedidoActualizarRequest;
import com.bateriasapp.dto.PedidoRequest;
import com.bateriasapp.dto.PedidoResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.model.Cliente;
import com.bateriasapp.model.EstadoPedido;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.model.Producto;
import com.bateriasapp.pricing.CalculadoraPrecio;
import com.bateriasapp.pricing.MetodoPago;
import com.bateriasapp.repository.ClienteRepository;
import com.bateriasapp.repository.PedidoRepository;
import com.bateriasapp.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Por qué la lógica vive aquí y no en el controller: el controller solo traduce HTTP;
 * las reglas de negocio (precio, estados) se prueban con Mockito sin levantar un servidor
 * y se reutilizan desde otros servicios (Entrega, Factura).
 * Por qué @Transactional: cada operación es atómica; si algo falla a mitad, no queda nada a medias.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final CalculadoraPrecio calculadoraPrecio;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         ProductoRepository productoRepository,
                         CalculadoraPrecio calculadoraPrecio) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.calculadoraPrecio = calculadoraPrecio;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return pedidoRepository.findAll().stream().map(PedidoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Integer id) {
        return PedidoResponse.desde(buscarEntidad(id));
    }

    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setCliente(buscarCliente(request.clienteId()));
        pedido.setProducto(buscarProductoDisponible(request.productoId()));
        pedido.setMetodoPago(aValorBd(request.metodoPago()));
        pedido.setEntregaBateriaUsada(request.entregaBateriaUsada());
        pedido.setPrecioFinal(calcularPrecio(pedido));
        pedido.setEstado(EstadoPedido.COTIZADO.getValor());
        pedido.setFechaCreacion(LocalDateTime.now());
        return PedidoResponse.desde(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse actualizar(Integer id, PedidoActualizarRequest request) {
        Pedido pedido = buscarEntidad(id);
        EstadoPedido estadoActual = EstadoPedido.desdeValor(pedido.getEstado());
        if (!estadoActual.esModificable()) {
            throw new ConflictoNegocioException("El pedido " + id + " está en estado '"
                    + estadoActual.getValor() + "' y no se puede modificar");
        }

        // Solo se recalcula si algo que afecta el precio realmente cambió; así un PUT sin cambios
        // no altera el precio pactado aunque el precio base del producto haya variado después.
        boolean recalcular = false;

        if (request.productoId() != null && !request.productoId().equals(pedido.getProducto().getId())) {
            pedido.setProducto(buscarProductoDisponible(request.productoId()));
            recalcular = true;
        }
        if (request.metodoPago() != null && !aValorBd(request.metodoPago()).equals(pedido.getMetodoPago())) {
            pedido.setMetodoPago(aValorBd(request.metodoPago()));
            recalcular = true;
        }
        if (request.entregaBateriaUsada() != null
                && !request.entregaBateriaUsada().equals(pedido.getEntregaBateriaUsada())) {
            pedido.setEntregaBateriaUsada(request.entregaBateriaUsada());
            recalcular = true;
        }
        if (recalcular) {
            pedido.setPrecioFinal(calcularPrecio(pedido));
        }
        return PedidoResponse.desde(pedidoRepository.save(pedido));
    }

    /**
     * Cambio manual de estado (PUT /api/pedidos/{id}/estado). FACTURADO queda fuera a propósito:
     * solo se alcanza generando la factura, para que nunca exista un pedido facturado sin factura.
     */
    @Transactional
    public PedidoResponse cambiarEstado(Integer id, EstadoRequest request) {
        Pedido pedido = buscarEntidad(id);
        EstadoPedido destino = EstadoPedido.desdeValor(request.estado());
        if (destino == EstadoPedido.FACTURADO) {
            throw new ConflictoNegocioException(
                    "El estado 'facturado' solo se alcanza generando la factura (POST /api/facturas)");
        }
        transicionar(pedido, destino);
        return PedidoResponse.desde(pedido);
    }

    /** Punto único de cambio de estado: lo usan este servicio y los de Entrega y Factura. */
    @Transactional
    public void transicionar(Pedido pedido, EstadoPedido destino) {
        EstadoPedido actual = EstadoPedido.desdeValor(pedido.getEstado());
        if (!actual.puedePasarA(destino)) {
            throw new ConflictoNegocioException("Transición no permitida: "
                    + actual.getValor() + " -> " + destino.getValor());
        }
        pedido.setEstado(destino.getValor());
        pedidoRepository.save(pedido);
    }

    @Transactional
    public void eliminar(Integer id) {
        pedidoRepository.delete(buscarEntidad(id));
    }

    /** Entidad para uso interno de otros servicios (nunca se expone por la API). */
    @Transactional(readOnly = true)
    public Pedido buscarEntidad(Integer id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", id));
    }

    /**
     * Por qué BigDecimal: double no representa decimales exactos (0.1 + 0.2 != 0.3);
     * con dinero eso produce pesos perdidos. La regla de redondeo vive en CalculadoraPrecio.
     */
    private BigDecimal calcularPrecio(Pedido pedido) {
        MetodoPago metodoPago = MetodoPago.valueOf(pedido.getMetodoPago().toUpperCase(Locale.ROOT));
        return calculadoraPrecio.calcularPrecioFinal(
                pedido.getProducto().getPrecioBase(), metodoPago, pedido.getEntregaBateriaUsada());
    }

    /** La BD guarda el método de pago en minúscula (CHECK de la tabla). Locale.ROOT evita el bug de la "i" turca. */
    private static String aValorBd(String metodoPago) {
        return metodoPago.toLowerCase(Locale.ROOT);
    }

    private Cliente buscarCliente(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private Producto buscarProductoDisponible(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
        if (!producto.isDisponible()) {
            throw new ConflictoNegocioException("El producto " + id + " no está disponible");
        }
        return producto;
    }
}
