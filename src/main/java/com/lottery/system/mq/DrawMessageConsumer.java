package com.lottery.system.mq;

import com.lottery.system.config.RabbitMqConfig;
import com.lottery.system.dto.DrawMessage;
import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import com.lottery.system.entity.UserActivityCounter;
import com.lottery.system.repository.DrawTicketRepository;
import com.lottery.system.repository.PrizeRepository;
import com.lottery.system.repository.UserActivityCounterRepository;
import com.lottery.system.enums.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DrawMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DrawMessageConsumer.class);

    private final DrawTicketRepository drawTicketRepository;
    private final PrizeRepository prizeRepository;
    private final UserActivityCounterRepository counterRepository;
    private final StringRedisTemplate redisTemplate;

    public DrawMessageConsumer(DrawTicketRepository drawTicketRepository,
                               PrizeRepository prizeRepository,
                               UserActivityCounterRepository counterRepository,
                               StringRedisTemplate redisTemplate) {
        this.drawTicketRepository = drawTicketRepository;
        this.prizeRepository = prizeRepository;
        this.counterRepository = counterRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Consumer for the main lottery draw queue.
     * Decrements physical stock in DB, increments user activity counter, and flags ticket as SUCCESS.
     * Uses JPA optimistic locking on Prize and UserActivityCounter.
     * @param message the DrawMessage payload
     */
    @RabbitListener(queues = RabbitMqConfig.DRAW_QUEUE)
    @Transactional
    public void consumeDrawMessage(DrawMessage message) {
        log.info("Consuming draw message for ticket: {}", message.getTicketId());

        DrawTicket ticket = drawTicketRepository.findById(message.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("DrawTicket not found: " + message.getTicketId()));

        Prize prize = prizeRepository.findById(message.getPrizeId())
                .orElseThrow(() -> new IllegalArgumentException("Prize not found: " + message.getPrizeId()));

        // If it's a physical prize, update the DB stock
        if (prize.getPrizeType() == 1) {
            if (prize.getStock() <= 0) {
                throw new IllegalStateException("Database stock depleted for prize: " + prize.getId());
            }
            prize.setStock(prize.getStock() - 1);
            prizeRepository.save(prize);
        }

        // Increment the activity participation count for this user
        UserActivityCounter counter = counterRepository.findByUserIdAndActivityId(message.getUserId(), message.getActivityId())
                .orElse(UserActivityCounter.builder()
                        .userId(message.getUserId())
                        .activityId(message.getActivityId())
                        .currentDrawCount(0)
                        .build());

        counter.setCurrentDrawCount(counter.getCurrentDrawCount() + 1);
        counterRepository.save(counter);

        // Update ticket to SUCCESS
        ticket.setStatus(TicketStatus.SUCCESS);
        drawTicketRepository.save(ticket);

        log.info("Successfully persisted draw result in database for ticket: {}", message.getTicketId());
    }

    /**
     * Consumer for the Dead Letter Queue (DLQ).
     * Handles failure compensation: refunds Redis stock and updates the ticket status to FAILED in the DB.
     * @param message the failed DrawMessage payload
     */
    @RabbitListener(queues = RabbitMqConfig.DLQ_QUEUE)
    @Transactional
    public void consumeDlaMessage(DrawMessage message) {
        log.error("Handling dead letter queue compensation for ticket: {}", message.getTicketId());

        // Update ticket status to SYSTEM_FAILED in database
        drawTicketRepository.findById(message.getTicketId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.SYSTEM_FAILED);
            drawTicketRepository.save(ticket);
        });

        // Compensate Redis stock if it's a physical prize
        prizeRepository.findById(message.getPrizeId()).ifPresent(prize -> {
            if (prize.getPrizeType() == 1) {
                String redisKey = "prize:" + prize.getId() + ":stock";
                redisTemplate.opsForValue().increment(redisKey);
                log.info("Compensated Redis stock for physical prize: {} via increment key {}", prize.getId(), redisKey);
            }
        });

        // Compensate Redis user draw count
        String userDrawKey = "user:draw:count:" + message.getActivityId() + ":" + message.getUserId();
        redisTemplate.opsForValue().decrement(userDrawKey);
        log.info("Compensated Redis user draw count for user: {} and activity: {}", message.getUserId(), message.getActivityId());
    }
}
