package com.bateriasapp.controller;

import com.bateriasapp.dto.BateriasSugeridasResponse;
import com.bateriasapp.service.SugerenciaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vive en su propio controller para no tocar el CRUD de /api/vehiculos que ya funciona;
 * Spring une ambos controllers bajo el mismo prefijo sin conflicto.
 */
@RestController
@RequestMapping("/api/vehiculos")
public class SugerenciaController {

    private final SugerenciaService sugerenciaService;

    public SugerenciaController(SugerenciaService sugerenciaService) {
        this.sugerenciaService = sugerenciaService;
    }

    @GetMapping("/{id}/baterias-sugeridas")
    public BateriasSugeridasResponse bateriasSugeridas(@PathVariable Integer id) {
        return sugerenciaService.sugerirParaVehiculo(id);
    }
}
