package com.medictime.medicos.service;

import com.medictime.medicos.config.RabbitConfig;
import com.medictime.medicos.model.Medico;
import com.medictime.medicos.repository.MedicoRepository;
import com.medictime.medicos.util.PasswordGenerator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final RabbitTemplate rabbitTemplate;

    public MedicoService(MedicoRepository medicoRepository, RabbitTemplate rabbitTemplate) {
        this.medicoRepository = medicoRepository;
        this.rabbitTemplate = rabbitTemplate;
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
        
        // 1. Guardar el médico en MySQL / H2
        Medico nuevoMedico = medicoRepository.save(medico);

        // 2. Generar contraseña provisoria
        String passwordProvisoria = PasswordGenerator.generarPasswordProvisoria(10);

        // 3. Disparar Evento a RabbitMQ con credenciales provisorias
        publicarEventoCreacion("notificacion.medico.creado", nuevoMedico, passwordProvisoria);

        return nuevoMedico;
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

    // Publicación del evento con datos de bienvenida y credenciales provisorias
    private void publicarEventoCreacion(String routingKey, Medico medico, String passwordProvisoria) {
        try {
            Map<String, Object> evento = new HashMap<>();
            evento.put("tipoEvento", "MEDICO_CREADO");
            evento.put("medicoId", medico.getId());
            evento.put("nombreCompleto", medico.getNombre() + " " + medico.getApellido());
            evento.put("email", medico.getEmail());
            evento.put("telefono", medico.getTelefono());
            evento.put("especialidad", medico.getEspecialidad());
            
            // 🔐 Credenciales Provisorias generadas por el sistema
            evento.put("username", medico.getEmail()); // El email actúa como nombre de usuario de inicio
            evento.put("passwordProvisoria", passwordProvisoria);
            evento.put("mensajeBienvenida", "Estimado/a Dr/a. " + medico.getApellido() + 
                    ", su cuenta en MedicTime ha sido creada con éxito. Inicie sesión con su clave provisoria y cámbiela en su primer ingreso.");

            rabbitTemplate.convertAndSend(
                    RabbitConfig.NOTIFICACIONES_EXCHANGE,
                    routingKey,
                    evento
            );
            
            System.out.println("✅ Evento 'MEDICO_CREADO' enviado a RabbitMQ con clave provisoria para: " + medico.getEmail());

        } catch (Exception e) {
            System.err.println("❌ Error publicando evento en RabbitMQ: " + e.getMessage());
        }
    }
}