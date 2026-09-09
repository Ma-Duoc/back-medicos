package com.medictime.medicos.controller;

import com.medictime.medicos.model.Medico;
import com.medictime.medicos.service.MedicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PrivateMedicoController {

    private final MedicoService medicoService;

    public PrivateMedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    // Endpoint de diagnóstico
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensaje", "Token válido en ms-medicos.");
        body.put("sub", jwt.getSubject());
        body.put("oid", jwt.getClaimAsString("oid"));
        body.put("usuario", jwt.getClaimAsString("preferred_username"));
        return body;
    }

    // Operaciones sensibles de negocio (CRUD protegido)
    @PostMapping("/medicos")
    public ResponseEntity<Medico> crearMedico(
            @Valid @RequestBody Medico medico,
            @AuthenticationPrincipal Jwt jwt) {
        return new ResponseEntity<>(medicoService.crear(medico), HttpStatus.CREATED);
    }

    @PutMapping("/medicos/{id}")
    public ResponseEntity<Medico> actualizarMedico(
            @PathVariable Long id,
            @Valid @RequestBody Medico medico,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(medicoService.actualizar(id, medico));
    }

    @DeleteMapping("/medicos/{id}")
    public ResponseEntity<Void> eliminarMedico(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        medicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/medicos")
    public ResponseEntity<List<Medico>> listarMedicosPrivado(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(medicoService.listarTodos());
    }
}