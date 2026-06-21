package com.lottery.system.service.draw.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.system.config.RabbitMqConfig;
import com.lottery.system.dto.DrawMessage;
import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PhysicalPrizeStrategy implements PrizeProcessStrategy {

    private static final Logger log = LoggerFactory.getLogger(PhysicalPrizeStrategy.class);

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PhysicalPrizeStrategy(StringRedisTemplate redisTemplate,
                                 RabbitTemplate rabbitTemplate,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean execute(DrawTicket ticket, Prize prize) {
        String stockKey = "prize:" + prize.getId() + ":stock";
        String outboxKey = "activity:" + ticket.getActivityId() + ":outbox";

        DrawMessage drawMessage = DrawMessage.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUserId())
                .activityId(ticket.getActivityId())
                .prizeId(prize.getId())
                .build();

        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(drawMessage);
        } catch (Exception e) {
            log.error("Failed to serialize DrawMessage to JSON", e);
            return false;
        }

        // Execute MULTI/EXEC transaction block in Redis
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) throws DataAccessException {
                operations.multi();
                operations.opsForValue().decrement((K) stockKey);
                operations.opsForList().leftPush((K) outboxKey, (V) messageJson);
                return operations.exec();
            }
        });

        if (txResults == null || txResults.isEmpty()) {
            log.error("Redis transaction failed for prize: {}", prize.getId());
            return false;
        }

        // First index of transaction result represents the DECR result
        Long remainingStock = (Long) txResults.get(0);

        if (remainingStock >= 0) {
            // Successfully reserved stock in Redis
            try {
                rabbitTemplate.convertAndSend(RabbitMqConfig.DRAW_EXCHANGE, RabbitMqConfig.DRAW_ROUTING_KEY, drawMessage);
                // On success, clean up outbox message
                redisTemplate.opsForList().remove(outboxKey, 1, messageJson);
            } catch (Exception e) {
                // If MQ publish fails, log the error and proceed. The scheduler will pick it up.
                log.error("Failed to send message to RabbitMQ for ticket: {}. Retaining in Outbox.", ticket.getTicketId(), e);
            }
            ticket.setPrizeId(prize.getId());
            return true;
        } else {
            // Stock was depleted: perform compensation increment and remove from outbox
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.opsForList().remove(outboxKey, 1, messageJson);
            return false;
        }
    }

    @Override
    public int getPrizeType() {
        return 1;
    }
}
