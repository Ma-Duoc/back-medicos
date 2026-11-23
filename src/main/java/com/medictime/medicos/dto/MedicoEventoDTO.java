package com.medictime.medicos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoEventoDTO {
    private String tipoEvento;      // MEDICO_CREADO, MEDICO_ACTUALIZADO, MEDICO_ELIMINADO
    private Long medicoId;
    private String nombreCompleto;
    private String email;           // Partition Key
    private String telefono;
    private String especialidad;
    private String estado;
    private String timestamp;
}