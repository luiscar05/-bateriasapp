package com.bateriasapp.service;

import com.bateriasapp.dto.EntregaActualizarRequest;
import com.bateriasapp.dto.EntregaEstadoRequest;
import com.bateriasapp.dto.EntregaRequest;
import com.bateriasapp.dto.EntregaResponse;
import com.bateriasapp.exception.ConflictoNegocioException;
import com.bateriasapp.exception.RecursoNoEncontradoException;
import com.bateriasapp.exception.SolicitudInvalidaException;
import com.bateriasapp.model.Domiciliario;
import com.bateriasapp.model.Entrega;
import com.bateriasapp.model.EstadoEntrega;
import com.bateriasapp.model.EstadoPedido;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.repository.DomiciliarioRepository;
import com.bateriasapp.repository.EntregaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * La entrega y el pedido se mantienen coherentes dentro de la misma transacción:
 * asignar una entrega pasa el pedido de PAGADO a EN_ENTREGA, y marcarla ENTREGADA
 * pasa el pedido a ENTREGADO. Así nadie puede dejar un estado a medias.
 */
@Service
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final DomiciliarioRepository domiciliarioRepository;
    private final PedidoService pedidoService;

    public EntregaService(EntregaRepository entregaRepository,
                          DomiciliarioRepository domiciliarioRepository,
                          PedidoService pedidoService) {
        this.entregaRepository = entregaRepository;
        this.domiciliarioRepository = domiciliarioRepository;
        this.pedidoService = pedidoService;
    }

    @Transactional(readOnly = true)
    public List<EntregaResponse> listar() {
        return entregaRepository.findAll().stream().map(EntregaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public EntregaResponse obtener(Integer id) {
        return EntregaResponse.desde(buscarEntrega(id));
    }

    @Transactional
    public EntregaResponse crear(EntregaRequest request) {
        Pedido pedido = pedidoService.buscarEntidad(request.pedidoId());
        EstadoPedido estadoPedido = EstadoPedido.desdeValor(pedido.getEstado());
        if (estadoPedido != EstadoPedido.PAGADO && estadoPedido != EstadoPedido.EN_ENTREGA) {
            throw new ConflictoNegocioException("Solo se puede asignar una entrega a un pedido pagado "
                    + "o en entrega; estado actual: " + estadoPedido.getValor());
        }
        if (entregaRepository.existsByPedidoIdAndEstadoNot(pedido.getId(), EstadoEntrega.FALLIDA.getValor())) {
            throw new ConflictoNegocioException("El pedido " + pedido.getId() + " ya tiene una entrega activa");
        }

        Entrega entrega = new Entrega();
        entrega.setPedido(pedido);
        entrega.setDomiciliario(buscarDomiciliario(request.domiciliarioId()));
        entrega.setDireccion(direccionOLaDelCliente(request.direccion(), pedido));
        entrega.setFechaEntrega(request.fechaEntrega());
        entrega.setInstalada(false);
        entrega.setEstado(EstadoEntrega.ASIGNADA.getValor());
        Entrega guardada = entregaRepository.save(entrega);

        if (estadoPedido == EstadoPedido.PAGADO) {
            pedidoService.transicionar(pedido, EstadoPedido.EN_ENTREGA);
        }
        return EntregaResponse.desde(guardada);
    }

    @Transactional
    public EntregaResponse actualizar(Integer id, EntregaActualizarRequest request) {
        Entrega entrega = buscarEntrega(id);
        EstadoEntrega estado = EstadoEntrega.desdeValor(entrega.getEstado());
        if (!estado.esModificable()) {
            throw new ConflictoNegocioException("La entrega " + id + " está en estado '"
                    + estado.getValor() + "' y no se puede modificar");
        }
        if (request.domiciliarioId() != null) {
            entrega.setDomiciliario(buscarDomiciliario(request.domiciliarioId()));
        }
        if (request.direccion() != null && !request.direccion().isBlank()) {
            entrega.setDireccion(request.direccion().trim());
        }
        if (request.fechaEntrega() != null) {
            entrega.setFechaEntrega(request.fechaEntrega());
        }
        return EntregaResponse.desde(entregaRepository.save(entrega));
    }

    @Transactional
    public EntregaResponse cambiarEstado(Integer id, EntregaEstadoRequest request) {
        Entrega entrega = buscarEntrega(id);
        EstadoEntrega actual = EstadoEntrega.desdeValor(entrega.getEstado());
        EstadoEntrega destino = EstadoEntrega.desdeValor(request.estado());

        if (!actual.puedePasarA(destino)) {
            throw new ConflictoNegocioException("Transición de entrega no permitida: "
                    + actual.getValor() + " -> " + destino.getValor());
        }
        if (request.instalada() != null && destino != EstadoEntrega.ENTREGADA) {
            throw new SolicitudInvalidaException("'instalada' solo se informa al marcar la entrega como entregada");
        }

        if (destino == EstadoEntrega.ENTREGADA) {
            // Primero el pedido: si no estuviera EN_ENTREGA, falla con 409 y nada se guarda.
            pedidoService.transicionar(entrega.getPedido(), EstadoPedido.ENTREGADO);
            entrega.setFechaEntrega(LocalDateTime.now());
            if (request.instalada() != null) {
                entrega.setInstalada(request.instalada());
            }
        }
        entrega.setEstado(destino.getValor());
        return EntregaResponse.desde(entregaRepository.save(entrega));
    }

    @Transactional
    public void eliminar(Integer id) {
        Entrega entrega = buscarEntrega(id);
        if (EstadoEntrega.desdeValor(entrega.getEstado()) != EstadoEntrega.ASIGNADA) {
            throw new ConflictoNegocioException("Solo se puede eliminar una entrega en estado 'asignada'");
        }
        entregaRepository.delete(entrega);
    }

    private Entrega buscarEntrega(Integer id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega", id));
    }

    private Domiciliario buscarDomiciliario(Integer id) {
        return domiciliarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Domiciliario", id));
    }

    private static String direccionOLaDelCliente(String direccion, Pedido pedido) {
        if (direccion == null || direccion.isBlank()) {
            return pedido.getCliente().getDireccion();
        }
        return direccion.trim();
    }
}
