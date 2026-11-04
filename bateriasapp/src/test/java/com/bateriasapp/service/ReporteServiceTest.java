package com.bateriasapp.service;

import com.bateriasapp.dto.PedidosPorEstadoResponse;
import com.bateriasapp.dto.ReporteVentasResponse;
import com.bateriasapp.dto.VentasPorMetodoPagoResponse;
import com.bateriasapp.exception.SolicitudInvalidaException;
import com.bateriasapp.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private BateriaUsadaService bateriaUsadaService;

    private ReporteService reporteService;

    @BeforeEach
    void preparar() {
        reporteService = new ReporteService(pedidoRepository, bateriaUsadaService);
    }

    @Test
    void ventas_sumaPorMetodoYRellenaConCeroLosMetodosSinVentas() {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"addi", 2L, new BigDecimal("497200.00")});
        filas.add(new Object[]{"efectivo", 1L, new BigDecimal("220000.00")});
        // El rango es inclusivo: hasta = 31/10 se consulta como "< 01/11 00:00".
        when(pedidoRepository.resumirVentasPorMetodoPago(
                eq(LocalDate.of(2026, 10, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 11, 1).atStartOfDay()),
                anyCollection())).thenReturn(filas);

        ReporteVentasResponse reporte = reporteService.ventas(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(reporte.totalPedidos()).isEqualTo(3);
        assertThat(reporte.totalVentas()).isEqualTo(new BigDecimal("717200"));
        assertThat(reporte.porMetodoPago()).extracting(VentasPorMetodoPagoResponse::metodoPago)
                .containsExactly("efectivo", "transferencia", "tarjeta", "addi");
        assertThat(reporte.porMetodoPago().get(1).cantidadPedidos()).isZero();
        assertThat(reporte.porMetodoPago().get(3).totalVentas()).isEqualTo(new BigDecimal("497200"));
    }

    @Test
    void ventas_desdePosteriorAHasta_lanza400SinConsultar() {
        assertThatThrownBy(() -> reporteService.ventas(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void pedidosPorEstado_incluyeLosSieteEstadosEnOrdenDelFlujo() {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"pagado", 4L});
        filas.add(new Object[]{"cotizado", 2L});
        when(pedidoRepository.contarPorEstado()).thenReturn(filas);

        List<PedidosPorEstadoResponse> reporte = reporteService.pedidosPorEstado();

        assertThat(reporte).extracting(PedidosPorEstadoResponse::estado).containsExactly(
                "cotizado", "confirmado", "pagado", "en_entrega", "entregado", "facturado", "cancelado");
        assertThat(reporte).extracting(PedidosPorEstadoResponse::cantidad).containsExactly(2L, 0L, 4L, 0L, 0L, 0L, 0L);
    }

    @Test
    void bateriasPendientes_delegaEnElServicioDeBateriasUsadas() {
        when(bateriaUsadaService.listarPendientes()).thenReturn(List.of());

        assertThat(reporteService.bateriasPendientesDeEvaluacion()).isEmpty();
    }
}
