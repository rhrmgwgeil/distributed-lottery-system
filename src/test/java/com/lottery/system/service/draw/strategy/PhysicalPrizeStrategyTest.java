package com.lottery.system.service.draw.strategy;

import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PhysicalPrizeStrategyTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private PhysicalPrizeStrategy physicalPrizeStrategy;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        physicalPrizeStrategy = new PhysicalPrizeStrategy(redisTemplate);
    }

    @Test
    public void testExecute_Success() {
        DrawTicket ticket = DrawTicket.builder().ticketId("ticket-1").build();
        Prize prize = Prize.builder().id(100L).prizeType(1).stock(10).build();

        // Mock Redis returning remaining stock >= 0 (e.g. 9 remaining)
        when(valueOperations.decrement("prize:stock:100")).thenReturn(9L);

        boolean result = physicalPrizeStrategy.execute(ticket, prize);

        assertTrue(result);
        verify(valueOperations, times(1)).decrement("prize:stock:100");
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    public void testExecute_OutOfStock_Downgrade() {
        DrawTicket ticket = DrawTicket.builder().ticketId("ticket-2").build();
        Prize prize = Prize.builder().id(100L).prizeType(1).stock(0).build();

        // Mock Redis returning remaining stock < 0 (e.g. -1 indicating out of stock)
        when(valueOperations.decrement("prize:stock:100")).thenReturn(-1L);
        when(valueOperations.increment("prize:stock:100")).thenReturn(0L);

        boolean result = physicalPrizeStrategy.execute(ticket, prize);

        assertFalse(result);
        verify(valueOperations, times(1)).decrement("prize:stock:100");
        // Verify compensation occurred
        verify(valueOperations, times(1)).increment("prize:stock:100");
    }
}
