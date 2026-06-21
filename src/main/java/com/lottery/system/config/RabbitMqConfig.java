package com.lottery.system.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String DRAW_EXCHANGE = "lottery.exchange";
    public static final String DRAW_QUEUE = "lottery.draw.queue";
    public static final String DRAW_ROUTING_KEY = "lottery.draw.routing";

    public static final String DLX_EXCHANGE = "lottery.dlx";
    public static final String DLQ_QUEUE = "lottery.dlq";
    public static final String DLQ_ROUTING_KEY = "lottery.dlq.routing";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange drawExchange() {
        return new TopicExchange(DRAW_EXCHANGE);
    }

    @Bean
    public Queue drawQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // Configure Dead Letter Exchange for this queue
        arguments.put("x-dead-letter-exchange", DLX_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
        return new Queue(DRAW_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding drawBinding(Queue drawQueue, TopicExchange drawExchange) {
        return BindingBuilder.bind(drawQueue).to(drawExchange).with(DRAW_ROUTING_KEY);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue dlqQueue() {
        return new Queue(DLQ_QUEUE, true, false, false);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange dlxExchange) {
        return BindingBuilder.bind(dlqQueue).to(dlxExchange).with(DLQ_ROUTING_KEY);
    }
}
