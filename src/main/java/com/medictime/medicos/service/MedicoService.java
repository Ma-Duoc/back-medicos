package com.medictime.medicos.service;

import com.medictime.medicos.config.RabbitConfig;
import com.medictime.medicos.dto.AuditoriaEventoDTO;
import com.medictime.medicos.dto.MedicoEventoDTO;
import com.medictime.medicos.model.Medico;
import com.medictime.medicos.repository.MedicoRepository;
import com.medictime.medicos.util.PasswordGenerator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${medictime.kafka.topic.medicos}")
    private String topicMedicos;

    @Value("${medictime.kafka.topic.auditoria}")
    private String topicAuditoria;

    public MedicoService(
            MedicoRepository medicoRepository,
            RabbitTemplate rabbitTemplate,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.medicoRepository = medicoRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
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

        // 3. RabbitMQ: Comando directo para correo/SMS con clave provisoria
        publicarComandoNotificacionRabbit("notificacion.medico.creado", nuevoMedico, passwordProvisoria);

        // 4. Kafka: Eventos de dominio y auditoría (Key = email)
        publicarEventoKafka("MEDICO_CREADO", nuevoMedico);
        publicarAuditoriaKafka("MEDICO_CREADO", nuevoMedico, "Creación de médico: " + nuevoMedico.getEmail());

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

        Medico actualizado = medicoRepository.save(medico);

        // Kafka: Eventos de dominio y auditoría (Key = email)
        publicarEventoKafka("MEDICO_ACTUALIZADO", actualizado);
        publicarAuditoriaKafka("MEDICO_ACTUALIZADO", actualizado, "Actualización de médico: " + actualizado.getEmail());

        return actualizado;
    }

    public void eliminar(Long id) {
        Medico medico = obtenerPorId(id);
        medicoRepository.delete(medico);

        // Kafka: Eventos de dominio y auditoría (Key = email)
        publicarEventoKafka("MEDICO_ELIMINADO", medico);
        publicarAuditoriaKafka("MEDICO_ELIMINADO", medico, "Eliminación de médico ID: " + id + " (" + medico.getEmail() + ")");
    }

    // 🐰 RabbitMQ: Comando directo para notificación asíncrona
    private void publicarComandoNotificacionRabbit(String routingKey, Medico medico, String passwordProvisoria) {
        try {
            Map<String, Object> evento = new HashMap<>();
            evento.put("tipoEvento", "MEDICO_CREADO");
            evento.put("medicoId", medico.getId());
            evento.put("nombreCompleto", medico.getNombre() + " " + medico.getApellido());
            evento.put("email", medico.getEmail());
            evento.put("telefono", medico.getTelefono());
            evento.put("especialidad", medico.getEspecialidad());
            evento.put("username", medico.getEmail());
            evento.put("passwordProvisoria", passwordProvisoria);
            evento.put("mensajeBienvenida", "Estimado/a Dr/a. " + medico.getApellido() +
                    ", su cuenta en MedicTime ha sido creada con éxito. Inicie sesión con su clave provisoria y cámbiela en su primer ingreso.");

            rabbitTemplate.convertAndSend(
                    RabbitConfig.NOTIFICACIONES_EXCHANGE,
                    routingKey,
                    evento
            );

            System.out.println("✅ Comando 'MEDICO_CREADO' enviado a RabbitMQ para: " + medico.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Error publicando comando en RabbitMQ: " + e.getMessage());
        }
    }

    // 🚀 Kafka: Evento de dominio tipado (Partition Key = email)
    private void publicarEventoKafka(String tipoEvento, Medico medico) {
        try {
            String partitionKey = medico.getEmail();

            MedicoEventoDTO dto = MedicoEventoDTO.builder()
                    .tipoEvento(tipoEvento)
                    .medicoId(medico.getId())
                    .nombreCompleto(medico.getNombre() + " " + medico.getApellido())
                    .email(medico.getEmail())
                    .telefono(medico.getTelefono())
                    .especialidad(medico.getEspecialidad())
                    .estado(medico.getEstado())
                    .timestamp(Instant.now().toString())
                    .build();

            kafkaTemplate.send(topicMedicos, partitionKey, dto)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            System.out.println("✅ Evento Kafka [" + tipoEvento + "] enviado a " + topicMedicos +
                                    " | Key (Email): " + partitionKey +
                                    " | Partición: " + result.getRecordMetadata().partition() +
                                    " | Offset: " + result.getRecordMetadata().offset());
                        } else {
                            System.err.println("❌ Error enviando evento a Kafka: " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("❌ Excepción publicando evento en Kafka: " + e.getMessage());
        }
    }

    // 🚀 Kafka: Registro de auditoría y trazabilidad (Partition Key = email)
    private void publicarAuditoriaKafka(String accion, Medico medico, String detalle) {
        try {
            String partitionKey = medico.getEmail();

            AuditoriaEventoDTO audit = AuditoriaEventoDTO.builder()
                    .servicio("ms-medicos")
                    .accion(accion)
                    .entidadId(medico.getId())
                    .emailMedico(medico.getEmail())
                    .usuarioOrigen("ms-medicos-api")
                    .detalle(detalle)
                    .timestamp(Instant.now().toString())
                    .build();

            kafkaTemplate.send(topicAuditoria, partitionKey, audit);
        } catch (Exception e) {
            System.err.println("⚠️ Error enviando auditoría a Kafka: " + e.getMessage());
        }
    }
}