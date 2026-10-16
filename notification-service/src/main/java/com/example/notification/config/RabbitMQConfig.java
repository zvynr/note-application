package com.example.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "notification.exchange";
    public static final String QUEUE_EMAIL = "notification.email.queue";
    public static final String ROUTING_KEY_EMAIL = "notification.email";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(QUEUE_EMAIL, true);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationExchange).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
