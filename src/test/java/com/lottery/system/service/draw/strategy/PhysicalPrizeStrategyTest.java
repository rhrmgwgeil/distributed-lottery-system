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

import com.lottery.system.repository.PrizeRepository;
import java.util.Optional;
import java.util.Arrays;

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
        private PrizeRepository prizeRepository;
        private PhysicalPrizeStrategy physicalPrizeStrategy;

        @BeforeEach
        @SuppressWarnings("unchecked")
        public void setUp() {
                redisTemplate = mock(StringRedisTemplate.class);
                listOperations = mock(ListOperations.class);
                valueOperations = mock(ValueOperations.class);

                when(redisTemplate.opsForList()).thenReturn(listOperations);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                // Default: stock key already exists in Redis
                when(redisTemplate.hasKey(anyString())).thenReturn(true);

                rabbitTemplate = mock(RabbitTemplate.class);
                objectMapper = new ObjectMapper();
                prizeRepository = mock(PrizeRepository.class);

                physicalPrizeStrategy = new PhysicalPrizeStrategy(redisTemplate, rabbitTemplate, objectMapper, prizeRepository);
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
                                eq(expectedMessage));

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
                verify(valueOperations, times(1)).increment("prize:100:stock");
                verify(listOperations, times(1)).remove(eq("activity:10:outbox"), eq(1L), anyString());
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testExecute_StockKeyMissing_WarmsUpFromDb() {
                DrawTicket ticket = DrawTicket.builder()
                                .ticketId("ticket-3")
                                .userId(1L)
                                .activityId(10L)
                                .build();
                Prize prize = Prize.builder()
                                .id(100L)
                                .prizeType(1)
                                .stock(5)
                                .build();

                // Mock Redis check: key is missing in Redis
                when(redisTemplate.hasKey("prize:100:stock")).thenReturn(false);

                // Mock PrizeRepository: returns DB prize with stock 5
                Prize dbPrize = Prize.builder()
                                .id(100L)
                                .stock(5)
                                .build();
                when(prizeRepository.findById(100L)).thenReturn(Optional.of(dbPrize));

                // Mock Redis transaction execution returning remaining stock 4
                when(redisTemplate.execute(any(SessionCallback.class))).thenReturn(Arrays.asList(4L));

                boolean result = physicalPrizeStrategy.execute(ticket, prize);

                assertTrue(result);

                // Verify DB prize lookup was triggered
                verify(prizeRepository, times(1)).findById(100L);
                // Verify setIfAbsent was executed with DB stock
                verify(valueOperations, times(1)).setIfAbsent("prize:100:stock", "5");
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testExecute_MqPublishFailure_RetainsInOutbox() {
                DrawTicket ticket = DrawTicket.builder()
                                .ticketId("ticket-4")
                                .userId(1L)
                                .activityId(10L)
                                .build();
                Prize prize = Prize.builder()
                                .id(100L)
                                .prizeType(1)
                                .stock(10)
                                .build();

                // Mock Redis check: key is present in Redis
                when(redisTemplate.hasKey("prize:100:stock")).thenReturn(true);

                // Mock Redis transaction execution returning remaining stock 9
                when(redisTemplate.execute(any(SessionCallback.class))).thenReturn(Arrays.asList(9L));

                // Mock rabbitTemplate to throw AmqpException (mocking MQ disconnect)
                doThrow(new org.springframework.amqp.AmqpException("Connection refused"))
                                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

                boolean result = physicalPrizeStrategy.execute(ticket, prize);

                // 1. Assert: Method still returns true (success) despite MQ exception
                assertTrue(result);

                // 2. Verify: rabbitTemplate was called
                verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));

                // 3. Verify: opsForList().remove() was NEVER called (message remains in Redis outbox)
                verify(listOperations, never()).remove(anyString(), anyLong(), anyString());
        }
}
