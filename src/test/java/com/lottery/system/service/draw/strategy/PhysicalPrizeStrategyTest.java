package com.lottery.system.service.draw.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.system.config.RabbitMqConfig;
import com.lottery.system.dto.DrawMessage;
import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PhysicalPrizeStrategyTest {

    private StringRedisTemplate redisTemplate;
    private ListOperations<String, String> listOperations;
    private ValueOperations<String, String> valueOperations;
    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;
    private PhysicalPrizeStrategy physicalPrizeStrategy;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        listOperations = mock(ListOperations.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rabbitTemplate = mock(RabbitTemplate.class);
        objectMapper = new ObjectMapper();

        physicalPrizeStrategy = new PhysicalPrizeStrategy(redisTemplate, rabbitTemplate, objectMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecute_Success() {
        DrawTicket ticket = DrawTicket.builder()
                .ticketId("ticket-1")
                .userId(1L)
                .activityId(10L)
                .build();
        Prize prize = Prize.builder()
                .id(100L)
                .prizeType(1)
                .stock(10)
                .build();

        // Mock Redis transaction execution returning remaining stock 9
        when(redisTemplate.execute(any(SessionCallback.class))).thenReturn(Arrays.asList(9L));

        boolean result = physicalPrizeStrategy.execute(ticket, prize);

        assertTrue(result);

        // Verify RabbitMQ publish was invoked
        DrawMessage expectedMessage = DrawMessage.builder()
                .ticketId("ticket-1")
                .userId(1L)
                .activityId(10L)
                .prizeId(100L)
                .build();
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqConfig.DRAW_EXCHANGE),
                eq(RabbitMqConfig.DRAW_ROUTING_KEY),
                eq(expectedMessage)
        );

        // Verify that the outbox message is cleaned up
        verify(listOperations, times(1)).remove(eq("activity:10:outbox"), eq(1L), anyString());
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecute_OutOfStock_Downgrade() {
        DrawTicket ticket = DrawTicket.builder()
                .ticketId("ticket-2")
                .userId(1L)
                .activityId(10L)
                .build();
        Prize prize = Prize.builder()
                .id(100L)
                .prizeType(1)
                .stock(0)
                .build();

        // Mock Redis transaction execution returning remaining stock -1
        when(redisTemplate.execute(any(SessionCallback.class))).thenReturn(Arrays.asList(-1L));

        boolean result = physicalPrizeStrategy.execute(ticket, prize);

        assertFalse(result);

        // Verify no MQ message sent
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));

        // Verify compensation: stock incremented back, and message removed from outbox
        verify(valueOperations, times(1)).increment("prize:stock:100");
        verify(listOperations, times(1)).remove(eq("activity:10:outbox"), eq(1L), anyString());
    }
}
