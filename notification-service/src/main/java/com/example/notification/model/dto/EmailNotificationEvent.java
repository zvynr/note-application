package com.example.notification.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

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
