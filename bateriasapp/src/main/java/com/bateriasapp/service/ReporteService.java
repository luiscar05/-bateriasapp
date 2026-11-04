package com.bateriasapp.service;

import com.bateriasapp.dto.BateriaUsadaResponse;
import com.bateriasapp.dto.PedidosPorEstadoResponse;
import com.bateriasapp.dto.ReporteVentasResponse;
import com.bateriasapp.dto.VentasPorMetodoPagoResponse;
import com.bateriasapp.exception.SolicitudInvalidaException;
import com.bateriasapp.model.EstadoPedido;
import com.bateriasapp.pricing.MetodoPago;
import com.bateriasapp.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReporteService {

    /** Una venta cuenta desde que el pedido está pagado; cotizados, confirmados y cancelados no suman. */
    private static final List<String> ESTADOS_DE_VENTA = List.of(
            EstadoPedido.PAGADO.getValor(),
            EstadoPedido.EN_ENTREGA.getValor(),
            EstadoPedido.ENTREGADO.getValor(),
            EstadoPedido.FACTURADO.getValor());

    private final PedidoRepository pedidoRepository;
    private final BateriaUsadaService bateriaUsadaService;

    public ReporteService(PedidoRepository pedidoRepository, BateriaUsadaService bateriaUsadaService) {
        this.pedidoRepository = pedidoRepository;
        this.bateriaUsadaService = bateriaUsadaService;
    }

    /** Rango por fecha de creación del pedido (la tabla no guarda fecha de pago); "hasta" es inclusive. */
    @Transactional(readOnly = true)
    public ReporteVentasResponse ventas(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("'desde' no puede ser posterior a 'hasta'");
        }
        List<Object[]> filas = pedidoRepository.resumirVentasPorMetodoPago(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), ESTADOS_DE_VENTA);

        Map<String, Object[]> filasPorMetodo = new HashMap<>();
        for (Object[] fila : filas) {
            filasPorMetodo.put((String) fila[0], fila);
        }

        List<VentasPorMetodoPagoResponse> porMetodoPago = new ArrayList<>();
        long totalPedidos = 0;
        BigDecimal totalVentas = BigDecimal.ZERO;
        for (MetodoPago metodoPago : MetodoPago.values()) {
            String valor = metodoPago.name().toLowerCase(Locale.ROOT);
            Object[] fila = filasPorMetodo.get(valor);
            long cantidad = fila == null ? 0 : ((Number) fila[1]).longValue();
            BigDecimal total = fila == null
                    ? BigDecimal.ZERO
                    : ((BigDecimal) fila[2]).setScale(0, RoundingMode.HALF_UP);
            porMetodoPago.add(new VentasPorMetodoPagoResponse(valor, cantidad, total));
            totalPedidos += cantidad;
            totalVentas = totalVentas.add(total);
        }
        return new ReporteVentasResponse(desde, hasta, totalPedidos, totalVentas, porMetodoPago);
    }

    @Transactional(readOnly = true)
    public List<BateriaUsadaResponse> bateriasPendientesDeEvaluacion() {
        return bateriaUsadaService.listarPendientes();
    }

    /** Incluye los siete estados, aun los que tienen cero pedidos, para que el reporte sea predecible. */
    @Transactional(readOnly = true)
    public List<PedidosPorEstadoResponse> pedidosPorEstado() {
        Map<String, Long> conteos = new HashMap<>();
        for (Object[] fila : pedidoRepository.contarPorEstado()) {
            conteos.put((String) fila[0], ((Number) fila[1]).longValue());
        }
        List<PedidosPorEstadoResponse> resultado = new ArrayList<>();
        for (EstadoPedido estado : EstadoPedido.values()) {
            resultado.add(new PedidosPorEstadoResponse(estado.getValor(), conteos.getOrDefault(estado.getValor(), 0L)));
        }
        return resultado;
    }
}
