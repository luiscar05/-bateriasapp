package com.bateriasapp.controller;

import com.bateriasapp.dto.BateriaUsadaResponse;
import com.bateriasapp.dto.PedidosPorEstadoResponse;
import com.bateriasapp.dto.ReporteVentasResponse;
import com.bateriasapp.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Fechas en formato ISO: ?desde=2026-10-01&hasta=2026-10-31 */
    @GetMapping("/ventas")
    public ReporteVentasResponse ventas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.ventas(desde, hasta);
    }

    @GetMapping("/baterias-usadas-pendientes")
    public List<BateriaUsadaResponse> bateriasUsadasPendientes() {
        return reporteService.bateriasPendientesDeEvaluacion();
    }

    @GetMapping("/pedidos-por-estado")
    public List<PedidosPorEstadoResponse> pedidosPorEstado() {
        return reporteService.pedidosPorEstado();
    }
}
