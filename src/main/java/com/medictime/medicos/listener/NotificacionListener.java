package com.medictime.medicos.listener;

import com.medictime.medicos.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificacionListener {

    @RabbitListener(queues = RabbitConfig.COLA_CORREO)
    public void procesarCorreo(Map<String, Object> evento) {
        System.out.println("\n------------------------------------------------------------");
        System.out.println("📧 [WORKER CORREO] Enviando email a: " + evento.get("email"));
        System.out.println("👤 Usuario: " + evento.get("username"));
        System.out.println("🔑 Clave Provisoria: " + evento.get("passwordProvisoria"));
        System.out.println("📄 Mensaje: " + evento.get("mensajeBienvenida"));
        System.out.println("------------------------------------------------------------\n");
        // Spring procesa esto con éxito y envía el ACK automático a RabbitMQ
    }

    @RabbitListener(queues = RabbitConfig.COLA_SMS)
    public void procesarSms(Map<String, Object> evento) {
        System.out.println("\n------------------------------------------------------------");
        System.out.println("📱 [WORKER SMS] Enviando SMS a: " + evento.get("telefono"));
        System.out.println("💬 MedicTime: Hola " + evento.get("nombreCompleto") +
                ", tu clave temporal de ingreso es: " + evento.get("passwordProvisoria"));
        System.out.println("------------------------------------------------------------\n");

        // Al lanzar una excepción, Spring hace NACK (requeue=false si está configurado
        // el DLX)
        // enviando el mensaje automáticamente a la cola de errores (cola.error.dlq)
        System.err.println("❌ [WORKER SMS] Simulación de error en SMS. Redirigiendo a DLQ...");
        throw new RuntimeException("Fallo intencional en pasarela SMS");
    }
}