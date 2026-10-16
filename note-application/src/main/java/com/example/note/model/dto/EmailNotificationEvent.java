package com.example.note.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Event published to RabbitMQ when an email should be sent.
 * Consumed asynchronously by notification-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String to;
    private String subject;
    private String body;
}
