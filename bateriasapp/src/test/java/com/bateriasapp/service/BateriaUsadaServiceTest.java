package com.bateriasapp.service;

import com.bateriasapp.DatosPrueba;
import com.bateriasapp.dto.BateriaUsadaEvaluacionRequest;
import com.bateriasapp.dto.BateriaUsadaRequest;
import com.bateriasapp.dto.BateriaUsadaResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.SolicitudInvalidaException;
import com.bateriasapp.model.BateriaUsada;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.repository.BateriaUsadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BateriaUsadaServiceTest {

    @Mock
    private BateriaUsadaRepository bateriaUsadaRepository;
    @Mock
    private PedidoService pedidoService;

    private BateriaUsadaService bateriaUsadaService;

    @BeforeEach
    void preparar() {
        bateriaUsadaService = new BateriaUsadaService(bateriaUsadaRepository, pedidoService);
    }

    private BateriaUsada bateria(int id, String estadoEvaluacion) {
        BateriaUsada bateria = new BateriaUsada();
        bateria.setId(id);
        bateria.setPedido(DatosPrueba.pedido(1, "entregado", "addi", true, "250000", "248600"));
        bateria.setEstadoEvaluacion(estadoEvaluacion);
        bateria.setFechaRecepcion(LocalDateTime.of(2026, 10, 27, 9, 0));
        return bateria;
    }

    @Test
    void registrar_pedidoEntregadoConBateria_quedaPendienteDeEvaluacion() {
        Pedido pedido = DatosPrueba.pedido(1, "entregado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);
        when(bateriaUsadaRepository.existsByPedidoId(1)).thenReturn(false);
        when(bateriaUsadaRepository.save(any(BateriaUsada.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        BateriaUsadaResponse respuesta = bateriaUsadaService.registrar(new BateriaUsadaRequest(1, "Bosch 60Ah", null));

        assertThat(respuesta.estadoEvaluacion()).isEqualTo("pendiente_evaluacion");
        assertThat(respuesta.destino()).isNull();
        assertThat(respuesta.fechaRecepcion()).isNotNull();
        assertThat(respuesta.descripcion()).isEqualTo("Bosch 60Ah");
    }

    @Test
    void registrar_pedidoSinEntregaDeBateria_lanzaConflicto() {
        Pedido pedido = DatosPrueba.pedido(1, "entregado", "efectivo", false, "250000", "250000");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);

        assertThatThrownBy(() -> bateriaUsadaService.registrar(new BateriaUsadaRequest(1, null, null)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("no incluye entrega de batería usada");
    }

    @Test
    void registrar_pedidoAunNoEnEntrega_lanzaConflicto() {
        Pedido pedido = DatosPrueba.pedido(1, "confirmado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);

        assertThatThrownBy(() -> bateriaUsadaService.registrar(new BateriaUsadaRequest(1, null, null)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("estado actual: confirmado");
    }

    @Test
    void registrar_bateriaYaRegistrada_lanzaConflicto() {
        Pedido pedido = DatosPrueba.pedido(1, "entregado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);
        when(bateriaUsadaRepository.existsByPedidoId(1)).thenReturn(true);

        assertThatThrownBy(() -> bateriaUsadaService.registrar(new BateriaUsadaRequest(1, null, null)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("ya tiene una batería usada");
        verify(bateriaUsadaRepository, never()).save(any());
    }

    @Test
    void evaluar_aceptadaConDestino_guardaResultadoYFecha() {
        BateriaUsada bateria = bateria(4, "pendiente_evaluacion");
        when(bateriaUsadaRepository.findById(4)).thenReturn(Optional.of(bateria));
        when(bateriaUsadaRepository.save(any(BateriaUsada.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        BateriaUsadaResponse respuesta = bateriaUsadaService.evaluar(4,
                new BateriaUsadaEvaluacionRequest("ACEPTADA", "LOTE_MENSUAL", "Carga correcta"));

        assertThat(respuesta.estadoEvaluacion()).isEqualTo("aceptada");
        assertThat(respuesta.destino()).isEqualTo("lote_mensual");
        assertThat(respuesta.fechaEvaluacion()).isNotNull();
        assertThat(respuesta.observaciones()).isEqualTo("Carga correcta");
    }

    @Test
    void evaluar_rechazadaSinDestino_esValido() {
        BateriaUsada bateria = bateria(4, "pendiente_evaluacion");
        when(bateriaUsadaRepository.findById(4)).thenReturn(Optional.of(bateria));
        when(bateriaUsadaRepository.save(any(BateriaUsada.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        BateriaUsadaResponse respuesta = bateriaUsadaService.evaluar(4,
                new BateriaUsadaEvaluacionRequest("rechazada", null, "Sulfatada"));

        assertThat(respuesta.estadoEvaluacion()).isEqualTo("rechazada");
        assertThat(respuesta.destino()).isNull();
    }

    @Test
    void evaluar_aceptadaSinDestino_lanza400() {
        when(bateriaUsadaRepository.findById(4)).thenReturn(Optional.of(bateria(4, "pendiente_evaluacion")));

        assertThatThrownBy(() -> bateriaUsadaService.evaluar(4,
                new BateriaUsadaEvaluacionRequest("aceptada", null, null)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("necesita destino");
    }

    @Test
    void evaluar_rechazadaConDestino_lanza400() {
        when(bateriaUsadaRepository.findById(4)).thenReturn(Optional.of(bateria(4, "pendiente_evaluacion")));

        assertThatThrownBy(() -> bateriaUsadaService.evaluar(4,
                new BateriaUsadaEvaluacionRequest("rechazada", "reventa", null)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void evaluar_bateriaYaEvaluada_lanzaConflicto() {
        when(bateriaUsadaRepository.findById(4)).thenReturn(Optional.of(bateria(4, "aceptada")));

        assertThatThrownBy(() -> bateriaUsadaService.evaluar(4,
                new BateriaUsadaEvaluacionRequest("rechazada", null, null)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("ya fue evaluada");
        verify(bateriaUsadaRepository, never()).save(any());
    }

    @Test
    void listarPendientes_consultaPorEstadoPendiente() {
        when(bateriaUsadaRepository.findByEstadoEvaluacionOrderByFechaRecepcionAsc("pendiente_evaluacion"))
                .thenReturn(List.of(bateria(4, "pendiente_evaluacion"), bateria(5, "pendiente_evaluacion")));

        assertThat(bateriaUsadaService.listarPendientes()).extracting(BateriaUsadaResponse::id).containsExactly(4, 5);
    }

    @Test
    void eliminar_bateriaEvaluada_lanzaConflicto() {
        when(bateriaUsadaRepository.findById(4)).thenReturn(Optional.of(bateria(4, "rechazada")));

        assertThatThrownBy(() -> bateriaUsadaService.eliminar(4)).isInstanceOf(ConflictoNegocioException.class);
        verify(bateriaUsadaRepository, never()).delete(any());
    }
}
