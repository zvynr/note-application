package com.example.note.service;

import com.example.note.configuration.RabbitMQConfig;
import com.example.note.model.dto.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes email notification events to RabbitMQ (fire-and-forget).
 * note-service does not wait for SMTP; notification-service consumes async.
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEmail(String to, String subject, String body) {
        EmailNotificationEvent event = new EmailNotificationEvent(to, subject, body);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_EMAIL,
                event
        );
    }
}
