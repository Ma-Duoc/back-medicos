package com.medictime.medicos.controller;

import com.medictime.medicos.model.Medico;
import com.medictime.medicos.service.MedicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/medicos")
public class PublicMedicoController {

    private final MedicoService medicoService;

    public PublicMedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @GetMapping
    public ResponseEntity<List<Medico>> listarMedicos() {
        return ResponseEntity.ok(medicoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medico> obtenerMedico(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.obtenerPorId(id));
    }
}
