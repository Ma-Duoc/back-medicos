package com.medictime.medicos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaEventoDTO {
    private String servicio;        // ms-medicos
    private String accion;          // MEDICO_CREADO, MEDICO_ACTUALIZADO, MEDICO_ELIMINADO
    private Long entidadId;
    private String emailMedico;     // Partition Key
    private String usuarioOrigen;
    private String detalle;
    private String timestamp;
}