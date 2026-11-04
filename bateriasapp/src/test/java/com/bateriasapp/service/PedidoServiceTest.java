package com.bateriasapp.service;

import com.bateriasapp.DatosPrueba;
import com.bateriasapp.dto.EstadoRequest;
import com.bateriasapp.dto.PedidoActualizarRequest;
import com.bateriasapp.dto.PedidoRequest;
import com.bateriasapp.dto.PedidoResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.pricing.CalculadoraPrecio;
import com.bateriasapp.repository.ClienteRepository;
import com.bateriasapp.repository.PedidoRepository;
import com.bateriasapp.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;

    private PedidoService pedidoService;

    @BeforeEach
    void preparar() {
        // La calculadora es real: es lógica pura y así el test valida el precio de punta a punta.
        pedidoService = new PedidoService(pedidoRepository, clienteRepository, productoRepository,
                new CalculadoraPrecio());
    }

    @Test
    void crear_casoDeReferencia_calcula248600YQuedaCotizado() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(DatosPrueba.cliente()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(DatosPrueba.producto(10, "250000.00", true)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoResponse respuesta = pedidoService.crear(new PedidoRequest(1, 10, "ADDI", true));

        assertThat(respuesta.precioFinal()).isEqualTo(new BigDecimal("248600"));
        assertThat(respuesta.estado()).isEqualTo("cotizado");
        assertThat(respuesta.metodoPago()).isEqualTo("addi");
    }

    @Test
    void crear_clienteInexistente_lanza404SinConsultarProducto() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.crear(new PedidoRequest(99, 10, "efectivo", false)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Cliente con id 99");
        verifyNoInteractions(productoRepository);
    }

    @Test
    void crear_productoNoDisponible_lanzaConflicto() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(DatosPrueba.cliente()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(DatosPrueba.producto(10, "250000", false)));

        assertThatThrownBy(() -> pedidoService.crear(new PedidoRequest(1, 10, "efectivo", false)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("no está disponible");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void actualizar_cambioDeMetodoDePago_recalculaElPrecio() {
        Pedido pedido = DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoResponse respuesta = pedidoService.actualizar(5, new PedidoActualizarRequest(null, "EFECTIVO", null));

        assertThat(respuesta.metodoPago()).isEqualTo("efectivo");
        assertThat(respuesta.precioFinal()).isEqualTo(new BigDecimal("220000"));
    }

    @Test
    void actualizar_cambioDeProducto_recalculaConElNuevoPrecioBase() {
        Pedido pedido = DatosPrueba.pedido(5, "confirmado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));
        when(productoRepository.findById(11)).thenReturn(Optional.of(DatosPrueba.producto(11, "100000", true)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoResponse respuesta = pedidoService.actualizar(5, new PedidoActualizarRequest(11, null, null));

        // (100.000 - 30.000) * 1,13 = 79.100
        assertThat(respuesta.precioFinal()).isEqualTo(new BigDecimal("79100"));
        assertThat(respuesta.productoId()).isEqualTo(11);
    }

    @Test
    void actualizar_cambioDeBateriaUsada_recalculaElPrecio() {
        Pedido pedido = DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoResponse respuesta = pedidoService.actualizar(5, new PedidoActualizarRequest(null, null, false));

        assertThat(respuesta.precioFinal()).isEqualTo(new BigDecimal("282500"));
    }

    @Test
    void actualizar_sinCambiosReales_conservaElPrecioPactado() {
        // El producto subió a 300.000 después de la cotización, pero el pedido pactó 248.600.
        Pedido pedido = DatosPrueba.pedido(5, "cotizado", "addi", true, "300000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoResponse respuesta = pedidoService.actualizar(5, new PedidoActualizarRequest(10, "addi", true));

        assertThat(respuesta.precioFinal()).isEqualTo(new BigDecimal("248600"));
    }

    @Test
    void actualizar_pedidoEntregado_lanzaConflicto() {
        Pedido pedido = DatosPrueba.pedido(5, "entregado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.actualizar(5, new PedidoActualizarRequest(null, "efectivo", null)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("no se puede modificar");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void actualizar_pedidoFacturadoOCancelado_lanzaConflicto() {
        Pedido facturado = DatosPrueba.pedido(5, "facturado", "addi", true, "250000", "248600");
        Pedido cancelado = DatosPrueba.pedido(6, "cancelado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(facturado));
        when(pedidoRepository.findById(6)).thenReturn(Optional.of(cancelado));

        assertThatThrownBy(() -> pedidoService.actualizar(5, new PedidoActualizarRequest(null, null, true)))
                .isInstanceOf(ConflictoNegocioException.class);
        assertThatThrownBy(() -> pedidoService.actualizar(6, new PedidoActualizarRequest(null, null, true)))
                .isInstanceOf(ConflictoNegocioException.class);
    }

    @Test
    void cambiarEstado_transicionValida_actualizaYGuarda() {
        Pedido pedido = DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));

        PedidoResponse respuesta = pedidoService.cambiarEstado(5, new EstadoRequest("CONFIRMADO"));

        assertThat(respuesta.estado()).isEqualTo("confirmado");
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void cambiarEstado_transicionInvalida_lanzaConflictoSinGuardar() {
        Pedido pedido = DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstado(5, new EstadoRequest("pagado")))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("cotizado -> pagado");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aFacturadoPorLaApi_lanzaConflictoAunqueElPedidoEsteEntregado() {
        Pedido pedido = DatosPrueba.pedido(5, "entregado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstado(5, new EstadoRequest("facturado")))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("generando la factura");
        assertThat(pedido.getEstado()).isEqualTo("entregado");
    }

    @Test
    void cambiarEstado_pedidoInexistente_lanza404() {
        when(pedidoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.cambiarEstado(99, new EstadoRequest("confirmado")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminar_pedidoExistente_loBorra() {
        Pedido pedido = DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));

        pedidoService.eliminar(5);

        verify(pedidoRepository).delete(pedido);
    }

    @Test
    void eliminar_pedidoInexistente_lanza404() {
        when(pedidoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.eliminar(99)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(pedidoRepository, never()).delete(any());
    }
}
