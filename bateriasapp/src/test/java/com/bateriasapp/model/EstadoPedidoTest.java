package com.bateriasapp.model;

import org.junit.jupiter.api.Test;

import static com.bateriasapp.model.EstadoPedido.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoPedidoTest {

    @Test
    void flujoCompletoAvanzaUnPasoALaVez() {
        assertThat(COTIZADO.puedePasarA(CONFIRMADO)).isTrue();
        assertThat(CONFIRMADO.puedePasarA(PAGADO)).isTrue();
        assertThat(PAGADO.puedePasarA(EN_ENTREGA)).isTrue();
        assertThat(EN_ENTREGA.puedePasarA(ENTREGADO)).isTrue();
        assertThat(ENTREGADO.puedePasarA(FACTURADO)).isTrue();
    }

    @Test
    void noSePermitenSaltosNiRetrocesos() {
        assertThat(COTIZADO.puedePasarA(PAGADO)).isFalse();
        assertThat(CONFIRMADO.puedePasarA(ENTREGADO)).isFalse();
        assertThat(PAGADO.puedePasarA(COTIZADO)).isFalse();
        assertThat(EN_ENTREGA.puedePasarA(PAGADO)).isFalse();
        assertThat(ENTREGADO.puedePasarA(CANCELADO)).isFalse();
    }

    @Test
    void cancelarSoloEstaPermitidoAntesDeEntregado() {
        assertThat(COTIZADO.puedePasarA(CANCELADO)).isTrue();
        assertThat(CONFIRMADO.puedePasarA(CANCELADO)).isTrue();
        assertThat(PAGADO.puedePasarA(CANCELADO)).isTrue();
        assertThat(EN_ENTREGA.puedePasarA(CANCELADO)).isTrue();
        assertThat(ENTREGADO.puedePasarA(CANCELADO)).isFalse();
        assertThat(FACTURADO.puedePasarA(CANCELADO)).isFalse();
    }

    @Test
    void facturadoYCanceladoSonTerminales() {
        for (EstadoPedido destino : EstadoPedido.values()) {
            assertThat(FACTURADO.puedePasarA(destino)).isFalse();
            assertThat(CANCELADO.puedePasarA(destino)).isFalse();
        }
    }

    @Test
    void soloEntregadoFacturadoYCanceladoNoSonModificables() {
        assertThat(COTIZADO.esModificable()).isTrue();
        assertThat(CONFIRMADO.esModificable()).isTrue();
        assertThat(PAGADO.esModificable()).isTrue();
        assertThat(EN_ENTREGA.esModificable()).isTrue();
        assertThat(ENTREGADO.esModificable()).isFalse();
        assertThat(FACTURADO.esModificable()).isFalse();
        assertThat(CANCELADO.esModificable()).isFalse();
    }

    @Test
    void desdeValor_ignoraMayusculasYRechazaDesconocidos() {
        assertThat(EstadoPedido.desdeValor("EN_ENTREGA")).isEqualTo(EN_ENTREGA);
        assertThat(EstadoPedido.desdeValor("cotizado")).isEqualTo(COTIZADO);
        assertThatThrownBy(() -> EstadoPedido.desdeValor("volando"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
