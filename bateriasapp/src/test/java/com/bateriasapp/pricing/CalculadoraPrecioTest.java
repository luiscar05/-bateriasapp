package com.bateriasapp.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraPrecioTest {

    private final CalculadoraPrecio calculadora = new CalculadoraPrecio();

    @Test
    void casoDeReferencia_addiConBateriaUsada_da248600() {
        BigDecimal precio = calculadora.calcularPrecioFinal(new BigDecimal("250000"), MetodoPago.ADDI, true);
        assertThat(precio).isEqualTo(new BigDecimal("248600"));
    }

    @Test
    void efectivoSinBateriaUsada_noCambiaElPrecio() {
        assertThat(calculadora.calcularPrecioFinal(new BigDecimal("250000"), MetodoPago.EFECTIVO, false))
                .isEqualTo(new BigDecimal("250000"));
    }

    @Test
    void transferenciaConBateriaUsada_soloDescuenta30000() {
        assertThat(calculadora.calcularPrecioFinal(new BigDecimal("250000"), MetodoPago.TRANSFERENCIA, true))
                .isEqualTo(new BigDecimal("220000"));
    }

    @Test
    void tarjetaSinBateriaUsada_recargo6PorCiento() {
        assertThat(calculadora.calcularPrecioFinal(new BigDecimal("250000"), MetodoPago.TARJETA, false))
                .isEqualTo(new BigDecimal("265000"));
    }

    @Test
    void addiSinBateriaUsada_recargo13PorCiento() {
        assertThat(calculadora.calcularPrecioFinal(new BigDecimal("250000"), MetodoPago.ADDI, false))
                .isEqualTo(new BigDecimal("282500"));
    }

    @Test
    void elSubtotalNuncaBajaDeCero() {
        assertThat(calculadora.calcularPrecioFinal(new BigDecimal("20000"), MetodoPago.ADDI, true))
                .isEqualTo(new BigDecimal("0"));
    }

    @Test
    void redondeaHalfUp_enLaMitad() {
        // 1025 + 6% (61,5) = 1086,5 -> 1087
        assertThat(calculadora.calcularPrecioFinal(new BigDecimal("1025"), MetodoPago.TARJETA, false))
                .isEqualTo(new BigDecimal("1087"));
    }

    @Test
    void aceptaPrecioBaseConDosDecimalesComoLoEntregaLaBd() {
        assertThat(calculadora.calcularPrecioFinal(new BigDecimal("250000.00"), MetodoPago.ADDI, true))
                .isEqualTo(new BigDecimal("248600"));
    }

    @Test
    void desglosar_casoDeReferencia() {
        DesglosePrecio desglose = calculadora.desglosar(new BigDecimal("250000"), MetodoPago.ADDI, true);
        assertThat(desglose.subtotal()).isEqualTo(new BigDecimal("220000"));
        assertThat(desglose.recargo()).isEqualTo(new BigDecimal("28600"));
        assertThat(desglose.total()).isEqualTo(new BigDecimal("248600"));
    }

    @Test
    void desglosarPrecioFinal_reconstruyeSubtotalYRecargo() {
        DesglosePrecio addi = calculadora.desglosarPrecioFinal(new BigDecimal("248600.00"), MetodoPago.ADDI);
        assertThat(addi.subtotal()).isEqualTo(new BigDecimal("220000"));
        assertThat(addi.recargo()).isEqualTo(new BigDecimal("28600"));

        DesglosePrecio tarjeta = calculadora.desglosarPrecioFinal(new BigDecimal("265000"), MetodoPago.TARJETA);
        assertThat(tarjeta.subtotal()).isEqualTo(new BigDecimal("250000"));
        assertThat(tarjeta.recargo()).isEqualTo(new BigDecimal("15000"));

        DesglosePrecio efectivo = calculadora.desglosarPrecioFinal(new BigDecimal("220000"), MetodoPago.EFECTIVO);
        assertThat(efectivo.subtotal()).isEqualTo(new BigDecimal("220000"));
        assertThat(efectivo.recargo()).isEqualTo(new BigDecimal("0"));
    }

    @Test
    void parametrosNulos_lanzanNullPointerException() {
        assertThatThrownBy(() -> calculadora.calcularPrecioFinal(null, MetodoPago.ADDI, true))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculadora.calcularPrecioFinal(new BigDecimal("1000"), null, true))
                .isInstanceOf(NullPointerException.class);
    }
}
