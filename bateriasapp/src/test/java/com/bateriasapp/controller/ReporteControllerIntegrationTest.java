package com.bateriasapp.controller;

import com.bateriasapp.exception.GlobalExceptionHandler;
import com.bateriasapp.repository.PedidoRepository;
import com.bateriasapp.service.BateriaUsadaService;
import com.bateriasapp.service.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReporteControllerIntegrationTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private BateriaUsadaService bateriaUsadaService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        ReporteService reporteService = new ReporteService(pedidoRepository, bateriaUsadaService);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReporteController(reporteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ventas_conRangoValido_responde200ConLosCuatroMetodos() throws Exception {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"addi", 1L, new BigDecimal("248600.00")});
        when(pedidoRepository.resumirVentasPorMetodoPago(any(), any(), anyCollection())).thenReturn(filas);

        mockMvc.perform(get("/api/reportes/ventas").param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(1))
                .andExpect(jsonPath("$.totalVentas").value(248600))
                .andExpect(jsonPath("$.porMetodoPago.length()").value(4))
                .andExpect(jsonPath("$.porMetodoPago[3].metodoPago").value("addi"));
    }

    @Test
    void ventas_sinParametros_responde400() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void ventas_fechaMalFormada_responde400() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas").param("desde", "01/10/2026").param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ventas_desdePosteriorAHasta_responde400() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas").param("desde", "2026-11-01").param("hasta", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(respuestaContiene("'desde'"));
    }

    @Test
    void pedidosPorEstado_devuelveLosSieteEstados() throws Exception {
        when(pedidoRepository.contarPorEstado()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/reportes/pedidos-por-estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].estado").value("cotizado"))
                .andExpect(jsonPath("$[0].cantidad").value(0));
    }

    @Test
    void bateriasUsadasPendientes_responde200() throws Exception {
        when(bateriaUsadaService.listarPendientes()).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/baterias-usadas-pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /** Verifica que el cuerpo JSON contenga un fragmento de texto (sin depender de Hamcrest). */
    private static ResultMatcher respuestaContiene(String fragmento) {
        return resultado -> assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains(fragmento);
    }
}
