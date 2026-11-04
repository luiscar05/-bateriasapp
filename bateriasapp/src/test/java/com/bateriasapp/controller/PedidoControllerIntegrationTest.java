package com.bateriasapp.controller;

import com.bateriasapp.DatosPrueba;
import com.bateriasapp.exception.GlobalExceptionHandler;
import com.bateriasapp.model.Pedido;
import com.bateriasapp.pricing.CalculadoraPrecio;
import com.bateriasapp.repository.ClienteRepository;
import com.bateriasapp.repository.PedidoRepository;
import com.bateriasapp.repository.ProductoRepository;
import com.bateriasapp.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la cadena real HTTP -> controller -> validación -> servicio -> manejo de errores.
 * Solo los repositorios son simulados, por eso corre en cualquier máquina sin MySQL.
 */
@ExtendWith(MockitoExtension.class)
class PedidoControllerIntegrationTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        PedidoService pedidoService = new PedidoService(pedidoRepository, clienteRepository, productoRepository,
                new CalculadoraPrecio());
        mockMvc = MockMvcBuilders.standaloneSetup(new PedidoController(pedidoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void post_casoDeReferencia_responde201Con248600() throws Exception {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(DatosPrueba.cliente()));
        when(productoRepository.findById(10)).thenReturn(Optional.of(DatosPrueba.producto(10, "250000.00", true)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": 1, "productoId": 10, "metodoPago": "addi", "entregaBateriaUsada": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioFinal").value(248600))
                .andExpect(jsonPath("$.estado").value("cotizado"))
                .andExpect(jsonPath("$.metodoPago").value("addi"))
                .andExpect(jsonPath("$.clienteNombre").value("Ana Gómez"));
    }

    @Test
    void post_cuerpoVacio_responde400ConDetallePorCampo() throws Exception {
        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles").isNotEmpty())
                .andExpect(jsonPath("$.ruta").value("/api/pedidos"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void post_metodoDePagoDesconocido_responde400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": 1, "productoId": 10, "metodoPago": "paypal", "entregaBateriaUsada": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(respuestaContiene("metodoPago"));
    }

    @Test
    void post_jsonMalFormado_responde400() throws Exception {
        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void get_pedidoInexistente_responde404ConJsonUniforme() throws Exception {
        when(pedidoRepository.findById(9999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pedidos/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Pedido con id 9999 no encontrado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void get_pedidoExistente_responde200() throws Exception {
        when(pedidoRepository.findById(5))
                .thenReturn(Optional.of(DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600.00")));

        mockMvc.perform(get("/api/pedidos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.precioFinal").value(248600));
    }

    @Test
    void putEstado_transicionInvalida_responde409() throws Exception {
        when(pedidoRepository.findById(5))
                .thenReturn(Optional.of(DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600")));

        mockMvc.perform(put("/api/pedidos/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"pagado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(respuestaContiene("cotizado -> pagado"));
    }

    @Test
    void putEstado_transicionValida_responde200() throws Exception {
        when(pedidoRepository.findById(5))
                .thenReturn(Optional.of(DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600")));

        mockMvc.perform(put("/api/pedidos/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"confirmado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("confirmado"));
    }

    @Test
    void putEstado_valorDesconocido_responde400() throws Exception {
        mockMvc.perform(put("/api/pedidos/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"volando\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putEstado_aFacturadoPorLaApi_responde409() throws Exception {
        when(pedidoRepository.findById(5))
                .thenReturn(Optional.of(DatosPrueba.pedido(5, "entregado", "addi", true, "250000", "248600")));

        mockMvc.perform(put("/api/pedidos/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"facturado\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void put_cambioDeMetodoDePago_recalculaYResponde200() throws Exception {
        when(pedidoRepository.findById(5))
                .thenReturn(Optional.of(DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(put("/api/pedidos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\": \"efectivo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioFinal").value(220000));
    }

    @Test
    void put_pedidoEntregado_responde409() throws Exception {
        when(pedidoRepository.findById(5))
                .thenReturn(Optional.of(DatosPrueba.pedido(5, "entregado", "addi", true, "250000", "248600")));

        mockMvc.perform(put("/api/pedidos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\": \"efectivo\"}"))
                .andExpect(status().isConflict())
                .andExpect(respuestaContiene("no se puede modificar"));
    }

    @Test
    void delete_pedidoExistente_responde204SinCuerpo() throws Exception {
        Pedido pedido = DatosPrueba.pedido(5, "cotizado", "addi", true, "250000", "248600");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));

        mockMvc.perform(delete("/api/pedidos/5"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(pedidoRepository).delete(pedido);
    }

    @Test
    void delete_pedidoInexistente_responde404() throws Exception {
        when(pedidoRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/pedidos/99")).andExpect(status().isNotFound());
    }

    /** Verifica que el cuerpo JSON contenga un fragmento de texto (sin depender de Hamcrest). */
    private static ResultMatcher respuestaContiene(String fragmento) {
        return resultado -> assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains(fragmento);
    }
}
