package com.bateriasapp.controller;

import com.bateriasapp.DatosPrueba;
import com.bateriasapp.exception.GlobalExceptionHandler;
import com.bateriasapp.model.ConsecutivoFactura;
import com.bateriasapp.model.Factura;
import com.bateriasapp.pricing.CalculadoraPrecio;
import com.bateriasapp.repository.ClienteRepository;
import com.bateriasapp.repository.ConsecutivoFacturaRepository;
import com.bateriasapp.repository.FacturaRepository;
import com.bateriasapp.repository.PedidoRepository;
import com.bateriasapp.repository.ProductoRepository;
import com.bateriasapp.service.FacturaService;
import com.bateriasapp.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FacturaControllerIntegrationTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private ConsecutivoFacturaRepository consecutivoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        CalculadoraPrecio calculadora = new CalculadoraPrecio();
        PedidoService pedidoService = new PedidoService(pedidoRepository, clienteRepository, productoRepository,
                calculadora);
        FacturaService facturaService = new FacturaService(facturaRepository, consecutivoRepository, pedidoService,
                calculadora);
        mockMvc = MockMvcBuilders.standaloneSetup(new FacturaController(facturaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void post_pedidoEntregado_responde201ConNumeroConsecutivoYDesglose() throws Exception {
        ConsecutivoFactura consecutivo = new ConsecutivoFactura();
        consecutivo.setId(1);
        consecutivo.setUltimoNumero(0);
        when(pedidoRepository.findById(7))
                .thenReturn(Optional.of(DatosPrueba.pedido(7, "entregado", "addi", true, "250000", "248600.00")));
        when(facturaRepository.existsByPedidoId(7)).thenReturn(false);
        when(consecutivoRepository.buscarConBloqueo(1)).thenReturn(Optional.of(consecutivo));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(post("/api/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\": 7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroFactura").value("FAC-000001"))
                .andExpect(jsonPath("$.subtotal").value(220000))
                .andExpect(jsonPath("$.recargo").value(28600))
                .andExpect(jsonPath("$.total").value(248600))
                .andExpect(jsonPath("$.pedidoId").value(7));
    }

    @Test
    void post_pedidoNoEntregado_responde409() throws Exception {
        when(pedidoRepository.findById(7))
                .thenReturn(Optional.of(DatosPrueba.pedido(7, "pagado", "addi", true, "250000", "248600")));

        mockMvc.perform(post("/api/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\": 7}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void post_sinPedidoId_responde400() throws Exception {
        mockMvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void get_facturaDeUnPedidoSinFactura_responde404() throws Exception {
        when(facturaRepository.findByPedidoId(3)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/facturas/pedido/3")).andExpect(status().isNotFound());
    }
}
