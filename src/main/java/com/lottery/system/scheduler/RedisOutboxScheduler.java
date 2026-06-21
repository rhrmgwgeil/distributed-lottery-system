package com.lottery.system.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.system.config.RabbitMqConfig;
import com.lottery.system.dto.DrawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@EnableScheduling
public class RedisOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(RedisOutboxScheduler.class);

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RedisOutboxScheduler(StringRedisTemplate redisTemplate,
                                RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Periodically scans Redis Outbox queues every 10 seconds to re-publish orphan messages to RabbitMQ.
     */
    @Scheduled(fixedDelay = 10000)
    public void processOutbox() {
        log.debug("Starting transactional outbox retry processing...");
        try {
            Set<String> outboxKeys = redisTemplate.keys("activity:*:outbox");
            if (outboxKeys == null || outboxKeys.isEmpty()) {
                return;
            }

            for (String outboxKey : outboxKeys) {
                log.debug("Processing outbox key: {}", outboxKey);
                processKeyQueue(outboxKey);
            }
        } catch (Exception e) {
            log.error("Error occurred while executing transactional outbox scheduler", e);
        }
    }

    private void processKeyQueue(String outboxKey) {
        while (true) {
            // Pop the oldest message from the tail (FIFO order)
            String messageJson = redisTemplate.opsForList().rightPop(outboxKey);
            if (messageJson == null) {
                break; // Queue is empty for this activity
            }

            DrawMessage drawMessage;
            try {
                drawMessage = objectMapper.readValue(messageJson, DrawMessage.class);
            } catch (Exception e) {
                log.error("Failed to deserialize outbox message JSON: {}. Deleting corrupted record.", messageJson, e);
                // Corrupted message: do not push back, skip to avoid infinite loop
                continue;
            }

            try {
                // Try sending to RabbitMQ
                rabbitTemplate.convertAndSend(RabbitMqConfig.DRAW_EXCHANGE, RabbitMqConfig.DRAW_ROUTING_KEY, drawMessage);
                log.info("Successfully re-published outbox message for ticket: {} to RabbitMQ", drawMessage.getTicketId());
            } catch (Exception e) {
                log.error("Failed to re-publish outbox message for ticket: {} to RabbitMQ. Re-queuing to tail.", drawMessage.getTicketId(), e);
                // Re-queue back to the outbox (to the head/left so it stays at the tail/FIFO next time)
                try {
                    redisTemplate.opsForList().leftPush(outboxKey, messageJson);
                } catch (Exception re) {
                    log.error("Failed to push message back to Redis Outbox list: {}", outboxKey, re);
                }
                // Break current queue loop because MQ is down
                break;
            }
        }
    }
}
