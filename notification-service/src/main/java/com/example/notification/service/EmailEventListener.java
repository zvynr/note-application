package com.example.notification.service;

import com.example.notification.config.RabbitMQConfig;
import com.example.notification.model.dto.EmailNotificationEvent;
import com.example.notification.model.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes email events from RabbitMQ and sends via SMTP.
 */
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmailEventListener.class);

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL)
    public void onEmailNotification(EmailNotificationEvent event) {
        log.info("Received email event for: {}", event.getTo());
        try {
            EmailRequest request = new EmailRequest();
            request.setTo(event.getTo());
            request.setSubject(event.getSubject());
            request.setBody(event.getBody());
            emailService.send(request);
            log.info("Email sent successfully to: {}", event.getTo());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", event.getTo(), e.getMessage());
            // Rethrow so message can be requeued / sent to DLQ if configured
            throw e;
        }
    }
}
