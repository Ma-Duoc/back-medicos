package com.medictime.medicos.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    // Nombres de Exchanges, Colas y Keys
    public static final String NOTIFICACIONES_EXCHANGE = "medictime.topic.exchange";
    public static final String ROUTING_KEY_PATRON = "notificacion.#";

    public static final String DLX_EXCHANGE = "medictime.dlx.exchange";
    public static final String COLA_ERROR = "cola.error.dlq";
    public static final String DLQ_ROUTING_KEY = "error.deadletter";

    public static final String COLA_CORREO = "cola.notificacion.correo";
    public static final String COLA_SMS = "cola.notificacion.sms";

    // 1. Convertidor JSON para que Spring publique objetos como JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 2. Dead Letter Exchange (DLX) y Cola de ERROR
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue colaError() {
        return QueueBuilder.durable(COLA_ERROR).build();
    }

    @Bean
    public Binding bindingDlx() {
        return BindingBuilder
                .bind(colaError())
                .to(dlxExchange())
                .with(DLQ_ROUTING_KEY);
    }

    // 3. Exchange Principal (Topic Exchange)
    @Bean
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(NOTIFICACIONES_EXCHANGE);
    }

    // Argumentos de Redirección a DLX
    private Map<String, Object> argumentosDlx() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
        return args;
    }

    // 4. Colas Principales
    @Bean
    public Queue colaCorreo() {
        return QueueBuilder.durable(COLA_CORREO)
                .withArguments(argumentosDlx())
                .build();
    }

    @Bean
    public Queue colaSms() {
        return QueueBuilder.durable(COLA_SMS)
                .withArguments(argumentosDlx())
                .build();
    }

    // 5. Enlaces (Bindings)
    @Bean
    public Binding bindingCorreo(Queue colaCorreo, TopicExchange notificacionesExchange) {
        return BindingBuilder.bind(colaCorreo).to(notificacionesExchange).with(ROUTING_KEY_PATRON);
    }

    @Bean
    public Binding bindingSms(Queue colaSms, TopicExchange notificacionesExchange) {
        return BindingBuilder.bind(colaSms).to(notificacionesExchange).with(ROUTING_KEY_PATRON);
    }
}