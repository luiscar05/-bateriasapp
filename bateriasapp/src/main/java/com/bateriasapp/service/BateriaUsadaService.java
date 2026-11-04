package com.bateriasapp.service;

import com.bateriasapp.dto.BateriaUsadaEvaluacionRequest;
import com.bateriasapp.dto.BateriaUsadaRequest;
import com.bateriasapp.dto.BateriaUsadaResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.exception.SolicitudInvalidaException;
import com.bateriasapp.model.BateriaUsada;
import com.bateriasapp.model.DestinoBateria;
import com.bateriasapp.model.EstadoEvaluacion;
import com.bateriasapp.model.EstadoPedido;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.repository.BateriaUsadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BateriaUsadaService {

    /** La batería se recibe físicamente cuando el domiciliario entrega el producto nuevo. */
    private static final Set<EstadoPedido> ESTADOS_CON_RECEPCION =
            Set.of(EstadoPedido.EN_ENTREGA, EstadoPedido.ENTREGADO, EstadoPedido.FACTURADO);

    private final BateriaUsadaRepository bateriaUsadaRepository;
    private final PedidoService pedidoService;

    public BateriaUsadaService(BateriaUsadaRepository bateriaUsadaRepository, PedidoService pedidoService) {
        this.bateriaUsadaRepository = bateriaUsadaRepository;
        this.pedidoService = pedidoService;
    }

    @Transactional(readOnly = true)
    public List<BateriaUsadaResponse> listar() {
        return bateriaUsadaRepository.findAll().stream().map(BateriaUsadaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public BateriaUsadaResponse obtener(Integer id) {
        return BateriaUsadaResponse.desde(buscarBateria(id));
    }

    @Transactional(readOnly = true)
    public List<BateriaUsadaResponse> listarPendientes() {
        return bateriaUsadaRepository
                .findByEstadoEvaluacionOrderByFechaRecepcionAsc(EstadoEvaluacion.PENDIENTE_EVALUACION.getValor())
                .stream().map(BateriaUsadaResponse::desde).toList();
    }

    @Transactional
    public BateriaUsadaResponse registrar(BateriaUsadaRequest request) {
        Pedido pedido = pedidoService.buscarEntidad(request.pedidoId());
        if (!Boolean.TRUE.equals(pedido.getEntregaBateriaUsada())) {
            throw new ConflictoNegocioException("El pedido " + pedido.getId() + " no incluye entrega de batería usada");
        }
        EstadoPedido estadoPedido = EstadoPedido.desdeValor(pedido.getEstado());
        if (!ESTADOS_CON_RECEPCION.contains(estadoPedido)) {
            throw new ConflictoNegocioException("La batería usada solo se registra cuando el pedido está en entrega, "
                    + "entregado o facturado; estado actual: " + estadoPedido.getValor());
        }
        if (bateriaUsadaRepository.existsByPedidoId(pedido.getId())) {
            throw new ConflictoNegocioException("El pedido " + pedido.getId() + " ya tiene una batería usada registrada");
        }

        BateriaUsada bateria = new BateriaUsada();
        bateria.setPedido(pedido);
        bateria.setDescripcion(request.descripcion());
        bateria.setObservaciones(request.observaciones());
        bateria.setEstadoEvaluacion(EstadoEvaluacion.PENDIENTE_EVALUACION.getValor());
        bateria.setFechaRecepcion(LocalDateTime.now());
        return BateriaUsadaResponse.desde(bateriaUsadaRepository.save(bateria));
    }

    /** La evaluación en bodega se hace una sola vez: aceptar exige destino, rechazar no lo admite. */
    @Transactional
    public BateriaUsadaResponse evaluar(Integer id, BateriaUsadaEvaluacionRequest request) {
        BateriaUsada bateria = buscarBateria(id);
        if (EstadoEvaluacion.desdeValor(bateria.getEstadoEvaluacion()) != EstadoEvaluacion.PENDIENTE_EVALUACION) {
            throw new ConflictoNegocioException("La batería " + id + " ya fue evaluada ("
                    + bateria.getEstadoEvaluacion() + ")");
        }

        EstadoEvaluacion resultado = EstadoEvaluacion.desdeValor(request.estadoEvaluacion());
        if (resultado == EstadoEvaluacion.ACEPTADA && request.destino() == null) {
            throw new SolicitudInvalidaException("Una batería aceptada necesita destino: lote_mensual o reventa");
        }
        if (resultado == EstadoEvaluacion.RECHAZADA && request.destino() != null) {
            throw new SolicitudInvalidaException("Una batería rechazada no tiene destino de venta");
        }

        bateria.setEstadoEvaluacion(resultado.getValor());
        if (request.destino() != null) {
            bateria.setDestino(DestinoBateria.desdeValor(request.destino()).getValor());
        }
        if (request.observaciones() != null) {
            bateria.setObservaciones(request.observaciones());
        }
        bateria.setFechaEvaluacion(LocalDateTime.now());
        return BateriaUsadaResponse.desde(bateriaUsadaRepository.save(bateria));
    }

    @Transactional
    public void eliminar(Integer id) {
        BateriaUsada bateria = buscarBateria(id);
        if (EstadoEvaluacion.desdeValor(bateria.getEstadoEvaluacion()) != EstadoEvaluacion.PENDIENTE_EVALUACION) {
            throw new ConflictoNegocioException("No se puede eliminar una batería que ya fue evaluada");
        }
        bateriaUsadaRepository.delete(bateria);
    }

    private BateriaUsada buscarBateria(Integer id) {
        return bateriaUsadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Batería usada", id));
    }
}
