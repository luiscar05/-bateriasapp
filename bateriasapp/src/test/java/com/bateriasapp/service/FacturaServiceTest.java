package com.bateriasapp.service;

import com.bateriasapp.DatosPrueba;
import com.bateriasapp.dto.FacturaRequest;
import com.bateriasapp.dto.FacturaResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.model.ConsecutivoFactura;
import com.bateriasapp.model.EstadoPedido;
import com.bateriasapp.model.Factura;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.pricing.CalculadoraPrecio;
import com.bateriasapp.repository.ConsecutivoFacturaRepository;
import com.bateriasapp.repository.FacturaRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private ConsecutivoFacturaRepository consecutivoRepository;
    @Mock
    private PedidoService pedidoService;

    private FacturaService facturaService;

    @BeforeEach
    void preparar() {
        facturaService = new FacturaService(facturaRepository, consecutivoRepository, pedidoService,
                new CalculadoraPrecio());
    }

    private ConsecutivoFactura consecutivo(int ultimo) {
        ConsecutivoFactura consecutivo = new ConsecutivoFactura();
        consecutivo.setId(1);
        consecutivo.setUltimoNumero(ultimo);
        return consecutivo;
    }

    @Test
    void generar_pedidoEntregado_emiteFacturaConNumeroConsecutivoYDesglose() {
        Pedido pedido = DatosPrueba.pedido(7, "entregado", "addi", true, "250000", "248600.00");
        ConsecutivoFactura consecutivo = consecutivo(4);
        when(pedidoService.buscarEntidad(7)).thenReturn(pedido);
        when(facturaRepository.existsByPedidoId(7)).thenReturn(false);
        when(consecutivoRepository.buscarConBloqueo(1)).thenReturn(Optional.of(consecutivo));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        FacturaResponse respuesta = facturaService.generar(new FacturaRequest(7));

        assertThat(respuesta.numeroFactura()).isEqualTo("FAC-000005");
        assertThat(respuesta.subtotal()).isEqualTo(new BigDecimal("220000"));
        assertThat(respuesta.recargo()).isEqualTo(new BigDecimal("28600"));
        assertThat(respuesta.total()).isEqualTo(new BigDecimal("248600"));
        assertThat(respuesta.pedidoId()).isEqualTo(7);
        assertThat(consecutivo.getUltimoNumero()).isEqualTo(5);
        verify(consecutivoRepository).save(consecutivo);
        verify(pedidoService).transicionar(pedido, EstadoPedido.FACTURADO);
    }

    @Test
    void generar_pedidoNoEntregado_lanzaConflictoSinTocarElConsecutivo() {
        Pedido pedido = DatosPrueba.pedido(7, "en_entrega", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(7)).thenReturn(pedido);

        assertThatThrownBy(() -> facturaService.generar(new FacturaRequest(7)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("Solo se factura un pedido entregado");
        verify(consecutivoRepository, never()).buscarConBloqueo(any());
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void generar_pedidoConFacturaExistente_lanzaConflicto() {
        Pedido pedido = DatosPrueba.pedido(7, "entregado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(7)).thenReturn(pedido);
        when(facturaRepository.existsByPedidoId(7)).thenReturn(true);

        assertThatThrownBy(() -> facturaService.generar(new FacturaRequest(7)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("ya tiene factura");
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void generar_sinFilaDeConsecutivo_fallaConMensajeClaro() {
        Pedido pedido = DatosPrueba.pedido(7, "entregado", "addi", true, "250000", "248600");
        when(pedidoService.buscarEntidad(7)).thenReturn(pedido);
        when(facturaRepository.existsByPedidoId(7)).thenReturn(false);
        when(consecutivoRepository.buscarConBloqueo(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facturaService.generar(new FacturaRequest(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db/migraciones.sql");
    }

    @Test
    void obtenerPorPedido_sinFactura_lanza404() {
        when(facturaRepository.findByPedidoId(3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facturaService.obtenerPorPedido(3)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
