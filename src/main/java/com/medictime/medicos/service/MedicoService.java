package com.medictime.medicos.service;

import com.medictime.medicos.model.Medico;
import com.medictime.medicos.repository.MedicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    public List<Medico> listarTodos() {
        return medicoRepository.findAll();
    }

    public Medico obtenerPorId(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado con ID: " + id));
    }

    public Medico crear(Medico medico) {
        if (medicoRepository.existsByEmail(medico.getEmail())) {
            throw new RuntimeException("El email ya está registrado: " + medico.getEmail());
        }
        return medicoRepository.save(medico);
    }

    public Medico actualizar(Long id, Medico detalles) {
        Medico medico = obtenerPorId(id);
        
        medico.setNombre(detalles.getNombre());
        medico.setApellido(detalles.getApellido());
        medico.setEspecialidad(detalles.getEspecialidad());
        medico.setEmail(detalles.getEmail());
        medico.setTelefono(detalles.getTelefono());
        medico.setEstado(detalles.getEstado());

        return medicoRepository.save(medico);
    }

    public void eliminar(Long id) {
        Medico medico = obtenerPorId(id);
        medicoRepository.delete(medico);
    }
}
