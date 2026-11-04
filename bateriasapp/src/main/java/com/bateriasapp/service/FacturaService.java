package com.bateriasapp.service;

import com.bateriasapp.dto.FacturaRequest;
import com.bateriasapp.dto.FacturaResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.model.ConsecutivoFactura;
import com.bateriasapp.model.EstadoPedido;
import com.bateriasapp.model.Factura;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.pricing.CalculadoraPrecio;
import com.bateriasapp.pricing.DesglosePrecio;
import com.bateriasapp.pricing.MetodoPago;
import com.bateriasapp.repository.ConsecutivoFacturaRepository;
import com.bateriasapp.repository.FacturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class FacturaService {

    private static final Integer ID_CONSECUTIVO = 1;
    private static final String PREFIJO = "FAC-";

    private final FacturaRepository facturaRepository;
    private final ConsecutivoFacturaRepository consecutivoRepository;
    private final PedidoService pedidoService;
    private final CalculadoraPrecio calculadoraPrecio;

    public FacturaService(FacturaRepository facturaRepository,
                          ConsecutivoFacturaRepository consecutivoRepository,
                          PedidoService pedidoService,
                          CalculadoraPrecio calculadoraPrecio) {
        this.facturaRepository = facturaRepository;
        this.consecutivoRepository = consecutivoRepository;
        this.pedidoService = pedidoService;
        this.calculadoraPrecio = calculadoraPrecio;
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listar() {
        return facturaRepository.findAll().stream().map(FacturaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtener(Integer id) {
        return FacturaResponse.desde(facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id)));
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtenerPorPedido(Integer pedidoId) {
        return FacturaResponse.desde(facturaRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura del pedido", pedidoId)));
    }

    /**
     * Por qué una tabla de consecutivo con bloqueo y no max(id)+1: dos facturas simultáneas
     * leerían el mismo máximo y generarían el mismo número. Con SELECT ... FOR UPDATE la segunda
     * espera a la primera; y si la transacción falla, el incremento también se deshace (sin huecos).
     */
    @Transactional
    public FacturaResponse generar(FacturaRequest request) {
        Pedido pedido = pedidoService.buscarEntidad(request.pedidoId());
        EstadoPedido estado = EstadoPedido.desdeValor(pedido.getEstado());
        if (estado != EstadoPedido.ENTREGADO) {
            throw new ConflictoNegocioException("Solo se factura un pedido entregado; estado actual: "
                    + estado.getValor());
        }
        if (facturaRepository.existsByPedidoId(pedido.getId())) {
            throw new ConflictoNegocioException("El pedido " + pedido.getId() + " ya tiene factura");
        }

        MetodoPago metodoPago = MetodoPago.valueOf(pedido.getMetodoPago().toUpperCase(Locale.ROOT));
        DesglosePrecio desglose = calculadoraPrecio.desglosarPrecioFinal(pedido.getPrecioFinal(), metodoPago);

        Factura factura = new Factura();
        factura.setPedido(pedido);
        factura.setNumeroFactura(siguienteNumero());
        factura.setFecha(LocalDateTime.now());
        factura.setSubtotal(desglose.subtotal());
        factura.setRecargo(desglose.recargo());
        factura.setTotal(desglose.total());
        Factura guardada = facturaRepository.save(factura);

        pedidoService.transicionar(pedido, EstadoPedido.FACTURADO);
        return FacturaResponse.desde(guardada);
    }

    private String siguienteNumero() {
        ConsecutivoFactura consecutivo = consecutivoRepository.buscarConBloqueo(ID_CONSECUTIVO)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta la fila id=1 en consecutivos_factura (ejecuta db/migraciones.sql)"));
        int siguiente = consecutivo.getUltimoNumero() + 1;
        consecutivo.setUltimoNumero(siguiente);
        consecutivoRepository.save(consecutivo);
        return PREFIJO + String.format("%06d", siguiente);
    }
}
