package com.bateriasapp.service;

import com.bateriasapp.DatosPrueba;
import com.bateriasapp.dto.EntregaActualizarRequest;
import com.bateriasapp.dto.EntregaEstadoRequest;
import com.bateriasapp.dto.EntregaRequest;
import com.bateriasapp.dto.EntregaResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.exception.SolicitudInvalidaException;
import com.bateriasapp.model.Entrega;
import com.bateriasapp.model.EstadoPedido;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.repository.DomiciliarioRepository;
import com.bateriasapp.repository.EntregaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

    private static final LocalDateTime MANANA = LocalDateTime.of(2026, 10, 28, 10, 0);

    @Mock
    private EntregaRepository entregaRepository;
    @Mock
    private DomiciliarioRepository domiciliarioRepository;
    @Mock
    private PedidoService pedidoService;

    private EntregaService entregaService;

    @BeforeEach
    void preparar() {
        entregaService = new EntregaService(entregaRepository, domiciliarioRepository, pedidoService);
    }

    private Entrega entrega(int id, Pedido pedido, String estado) {
        Entrega entrega = new Entrega();
        entrega.setId(id);
        entrega.setPedido(pedido);
        entrega.setDomiciliario(DatosPrueba.domiciliario(2));
        entrega.setDireccion("Carrera 7 # 40-10");
        entrega.setFechaEntrega(MANANA);
        entrega.setInstalada(false);
        entrega.setEstado(estado);
        return entrega;
    }

    @Test
    void crear_pedidoPagado_asignaEntregaYLoPasaAEnEntrega() {
        Pedido pedido = DatosPrueba.pedido(1, "pagado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);
        when(entregaRepository.existsByPedidoIdAndEstadoNot(1, "fallida")).thenReturn(false);
        when(domiciliarioRepository.findById(2)).thenReturn(Optional.of(DatosPrueba.domiciliario(2)));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaResponse respuesta = entregaService.crear(new EntregaRequest(1, 2, null, MANANA));

        assertThat(respuesta.estado()).isEqualTo("asignada");
        assertThat(respuesta.instalada()).isFalse();
        assertThat(respuesta.direccion()).isEqualTo("Calle 100 # 15-20"); // dirección del cliente
        verify(pedidoService).transicionar(pedido, EstadoPedido.EN_ENTREGA);
    }

    @Test
    void crear_conDireccionPropia_laRespeta() {
        Pedido pedido = DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);
        when(entregaRepository.existsByPedidoIdAndEstadoNot(1, "fallida")).thenReturn(false);
        when(domiciliarioRepository.findById(2)).thenReturn(Optional.of(DatosPrueba.domiciliario(2)));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaResponse respuesta = entregaService.crear(new EntregaRequest(1, 2, "  Av. 68 # 20-30  ", MANANA));

        assertThat(respuesta.direccion()).isEqualTo("Av. 68 # 20-30");
        verify(pedidoService, never()).transicionar(any(), any()); // ya estaba en entrega (reasignación)
    }

    @Test
    void crear_pedidoSinPagar_lanzaConflicto() {
        Pedido pedido = DatosPrueba.pedido(1, "confirmado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);

        assertThatThrownBy(() -> entregaService.crear(new EntregaRequest(1, 2, null, MANANA)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("pagado");
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void crear_pedidoConEntregaActiva_lanzaConflicto() {
        Pedido pedido = DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);
        when(entregaRepository.existsByPedidoIdAndEstadoNot(1, "fallida")).thenReturn(true);

        assertThatThrownBy(() -> entregaService.crear(new EntregaRequest(1, 2, null, MANANA)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("entrega activa");
    }

    @Test
    void crear_domiciliarioInexistente_lanza404() {
        Pedido pedido = DatosPrueba.pedido(1, "pagado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(1)).thenReturn(pedido);
        when(entregaRepository.existsByPedidoIdAndEstadoNot(1, "fallida")).thenReturn(false);
        when(domiciliarioRepository.findById(77)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.crear(new EntregaRequest(1, 77, null, MANANA)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Domiciliario");
    }

    @Test
    void cambiarEstado_asignadaAEnCamino_esValido() {
        Entrega entrega = entrega(3, DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600"), "asignada");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(entrega));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaResponse respuesta = entregaService.cambiarEstado(3, new EntregaEstadoRequest("en_camino", null));

        assertThat(respuesta.estado()).isEqualTo("en_camino");
    }

    @Test
    void cambiarEstado_aEntregada_pasaElPedidoAEntregadoYMarcaInstalada() {
        Pedido pedido = DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600");
        Entrega entrega = entrega(3, pedido, "en_camino");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(entrega));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaResponse respuesta = entregaService.cambiarEstado(3, new EntregaEstadoRequest("ENTREGADA", true));

        assertThat(respuesta.estado()).isEqualTo("entregada");
        assertThat(respuesta.instalada()).isTrue();
        assertThat(respuesta.fechaEntrega()).isNotEqualTo(MANANA); // se reemplaza por la fecha real
        verify(pedidoService).transicionar(pedido, EstadoPedido.ENTREGADO);
    }

    @Test
    void cambiarEstado_saltoInvalido_lanzaConflicto() {
        Entrega entrega = entrega(3, DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600"), "asignada");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.cambiarEstado(3, new EntregaEstadoRequest("entregada", null)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("asignada -> entregada");
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_instaladaFueraDeLaEntrega_lanza400() {
        Entrega entrega = entrega(3, DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600"), "asignada");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.cambiarEstado(3, new EntregaEstadoRequest("en_camino", true)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void cambiarEstado_aFallida_dejaElPedidoEnEntregaParaReasignar() {
        Entrega entrega = entrega(3, DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600"), "en_camino");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(entrega));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaResponse respuesta = entregaService.cambiarEstado(3, new EntregaEstadoRequest("fallida", null));

        assertThat(respuesta.estado()).isEqualTo("fallida");
        verify(pedidoService, never()).transicionar(any(), any());
    }

    @Test
    void actualizar_entregaAbierta_cambiaDomiciliarioYDireccion() {
        Entrega entrega = entrega(3, DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600"), "asignada");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(entrega));
        when(domiciliarioRepository.findById(8)).thenReturn(Optional.of(DatosPrueba.domiciliario(8)));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaResponse respuesta = entregaService.actualizar(3, new EntregaActualizarRequest(8, "Calle 80 # 10-10", null));

        assertThat(respuesta.domiciliarioId()).isEqualTo(8);
        assertThat(respuesta.direccion()).isEqualTo("Calle 80 # 10-10");
        assertThat(respuesta.fechaEntrega()).isEqualTo(MANANA);
    }

    @Test
    void actualizar_entregaCerrada_lanzaConflicto() {
        Entrega entrega = entrega(3, DatosPrueba.pedido(1, "entregado", "addi", true, "250000", "248600"), "entregada");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.actualizar(3, new EntregaActualizarRequest(null, "Otra", null)))
                .isInstanceOf(ConflictoNegocioException.class);
    }

    @Test
    void eliminar_soloSiEstaAsignada() {
        Entrega asignada = entrega(3, DatosPrueba.pedido(1, "en_entrega", "addi", true, "250000", "248600"), "asignada");
        Entrega enCamino = entrega(4, DatosPrueba.pedido(2, "en_entrega", "addi", true, "250000", "248600"), "en_camino");
        when(entregaRepository.findById(3)).thenReturn(Optional.of(asignada));
        when(entregaRepository.findById(4)).thenReturn(Optional.of(enCamino));

        entregaService.eliminar(3);
        verify(entregaRepository).delete(asignada);

        assertThatThrownBy(() -> entregaService.eliminar(4)).isInstanceOf(ConflictoNegocioException.class);
        verify(entregaRepository, never()).delete(enCamino);
    }

    @Test
    void obtener_inexistente_lanza404() {
        when(entregaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.obtener(99)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
