package com.bateriasapp.service;

import com.bateriasapp.dto.BateriasSugeridasResponse;
import com.bateriasapp.dto.ProductoSugeridoResponse;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.model.Producto;
import com.bateriasapp.model.Vehiculo;
import com.bateriasapp.repository.ProductoRepository;
import com.bateriasapp.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * SOLO SUGIERE: devuelve candidatos y deja la decisión a una persona; no crea pedidos ni reserva stock.
 * Criterio de compatibilidad (supuesto documentado en el README):
 * amperaje entre el recomendado y el recomendado + 30 %, y CCA igual o mayor al recomendado.
 * Si el vehículo no tiene un dato de referencia, ese criterio no se aplica.
 */
@Service
public class SugerenciaService {

    private static final int PORCENTAJE_MAXIMO_SOBRE_AMPERAJE = 130;
    private static final String AVISO =
            "Sugerencia automática basada en la tabla de referencia. Una persona debe confirmar la batería.";
    private static final String AVISO_SIN_REFERENCIA =
            "El vehículo no tiene amperaje ni CCA de referencia, por lo que no se puede sugerir. "
                    + "Una persona debe elegir la batería.";

    private final VehiculoRepository vehiculoRepository;
    private final ProductoRepository productoRepository;

    public SugerenciaService(VehiculoRepository vehiculoRepository, ProductoRepository productoRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public BateriasSugeridasResponse sugerirParaVehiculo(Integer vehiculoId) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", vehiculoId));

        boolean sinReferencia = vehiculo.getAmperajeRecomendado() == null && vehiculo.getCcaRecomendado() == null;
        List<ProductoSugeridoResponse> sugerencias = sinReferencia
                ? List.of()
                : productoRepository.buscarBateriasDisponibles().stream()
                        .filter(producto -> esCompatible(vehiculo, producto))
                        .sorted(Comparator.comparingInt(Producto::getAmperaje)
                                .thenComparingInt(Producto::getCca)
                                .thenComparing(Producto::getPrecioBase))
                        .map(ProductoSugeridoResponse::desde)
                        .toList();

        return new BateriasSugeridasResponse(vehiculo.getId(), vehiculo.getMarca(), vehiculo.getModelo(),
                vehiculo.getAnio(), vehiculo.getAmperajeRecomendado(), vehiculo.getCcaRecomendado(),
                sinReferencia ? AVISO_SIN_REFERENCIA : AVISO, sugerencias);
    }

    private boolean esCompatible(Vehiculo vehiculo, Producto bateria) {
        Integer amperajeMinimo = vehiculo.getAmperajeRecomendado();
        if (amperajeMinimo != null) {
            int amperajeMaximo = amperajeMinimo * PORCENTAJE_MAXIMO_SOBRE_AMPERAJE / 100;
            if (bateria.getAmperaje() < amperajeMinimo || bateria.getAmperaje() > amperajeMaximo) {
                return false;
            }
        }
        Integer ccaMinimo = vehiculo.getCcaRecomendado();
        return ccaMinimo == null || bateria.getCca() >= ccaMinimo;
    }
}
