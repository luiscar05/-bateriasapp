package com.bateriasapp.service;

import com.bateriasapp.DatosPrueba;
import com.bateriasapp.dto.BateriasSugeridasResponse;
import com.bateriasapp.dto.ProductoSugeridoResponse;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.model.Producto;
import com.bateriasapp.model.Vehiculo;
import com.bateriasapp.repository.ProductoRepository;
import com.bateriasapp.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SugerenciaServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private ProductoRepository productoRepository;

    private SugerenciaService sugerenciaService;

    @BeforeEach
    void preparar() {
        sugerenciaService = new SugerenciaService(vehiculoRepository, productoRepository);
    }

    private Vehiculo vehiculo(Integer amperaje, Integer cca) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(1);
        vehiculo.setMarca("Renault");
        vehiculo.setModelo("Logan");
        vehiculo.setAnio(2018);
        vehiculo.setAmperajeRecomendado(amperaje);
        vehiculo.setCcaRecomendado(cca);
        return vehiculo;
    }

    private Producto bateria(int id, int amperaje, int cca, String precio) {
        Producto producto = DatosPrueba.producto(id, precio, true);
        producto.setAmperaje(amperaje);
        producto.setCca(cca);
        return producto;
    }

    @Test
    void sugerir_filtraPorAmperajeYCcaYOrdenaDeMenorAMayor() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(60, 540)));
        when(productoRepository.buscarBateriasDisponibles()).thenReturn(List.of(
                bateria(1, 70, 600, "300000"),   // compatible (60..78 Ah, CCA >= 540)
                bateria(2, 60, 540, "250000"),   // compatible, la más ajustada
                bateria(3, 90, 700, "400000"),   // amperaje demasiado alto
                bateria(4, 55, 500, "200000"),   // amperaje menor al recomendado
                bateria(5, 62, 500, "220000"))); // CCA insuficiente

        BateriasSugeridasResponse respuesta = sugerenciaService.sugerirParaVehiculo(1);

        assertThat(respuesta.sugerencias()).extracting(ProductoSugeridoResponse::id).containsExactly(2, 1);
        assertThat(respuesta.aviso()).contains("persona debe confirmar");
        assertThat(respuesta.marca()).isEqualTo("Renault");
        assertThat(respuesta.amperajeRecomendado()).isEqualTo(60);
    }

    @Test
    void sugerir_conSoloCcaDeReferencia_ignoraElAmperaje() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(null, 540)));
        when(productoRepository.buscarBateriasDisponibles()).thenReturn(List.of(
                bateria(1, 90, 700, "400000"),
                bateria(2, 45, 400, "150000")));

        BateriasSugeridasResponse respuesta = sugerenciaService.sugerirParaVehiculo(1);

        assertThat(respuesta.sugerencias()).extracting(ProductoSugeridoResponse::id).containsExactly(1);
    }

    @Test
    void sugerir_vehiculoSinReferencia_devuelveListaVaciaConAvisoYNoConsultaProductos() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo(null, null)));

        BateriasSugeridasResponse respuesta = sugerenciaService.sugerirParaVehiculo(1);

        assertThat(respuesta.sugerencias()).isEmpty();
        assertThat(respuesta.aviso()).contains("no tiene amperaje ni CCA de referencia");
        verifyNoInteractions(productoRepository);
    }

    @Test
    void sugerir_vehiculoInexistente_lanza404() {
        when(vehiculoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sugerenciaService.sugerirParaVehiculo(99))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
