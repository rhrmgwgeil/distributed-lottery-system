package com.lottery.system.mq;

import com.lottery.system.config.RabbitMqConfig;
import com.lottery.system.dto.DrawMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DrawMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public DrawMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendDrawMessage(String ticketId, Long userId, Long activityId, Long prizeId) {
        DrawMessage message = DrawMessage.builder()
                .ticketId(ticketId)
                .userId(userId)
                .activityId(activityId)
                .prizeId(prizeId)
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.DRAW_EXCHANGE, RabbitMqConfig.DRAW_ROUTING_KEY, message);
    }
}
