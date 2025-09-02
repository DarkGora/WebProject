package org.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {
    public static final String MAIL_QUEUE = "mailQueue";
    public static final String MAIL_EXCHANGE = "mailExchange";
    public static final String MAIL_ROUTING_KEY = "mail.routing.key";
    public static final String BAGiS_QUEUE = "BagisQueue";
    public static final String BAGiS_EXCHANGE = "BagixExchange";
    public static final String BAGiS_ROUTING_KEY = "Bagis.routing.key";

    @Bean
    public Queue mailQueue() {
        return new Queue(MAIL_QUEUE, false);
    }

    @Bean
    public Queue bagisQueue() {
        return new Queue(BAGiS_QUEUE, false);
    }

    @Bean
    public DirectExchange mailExchange() {
        return new DirectExchange(MAIL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange bagisExchange() {
        return new DirectExchange(BAGiS_EXCHANGE, true, false);
    }

    @Bean
    public Binding bagisbinding(Queue bagisQueue, DirectExchange bagisExchange) {
        return BindingBuilder.bind(bagisQueue)
                .to(bagisExchange)
                .with(BAGiS_ROUTING_KEY);
    }

    @Bean
    public Binding mailBinding(Queue mailQueue, DirectExchange mailExchange) {
        return BindingBuilder.bind(mailQueue)
                .to(mailExchange)
                .with(MAIL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ВАЖНО: Настройте RabbitTemplate с JSON конвертером
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}