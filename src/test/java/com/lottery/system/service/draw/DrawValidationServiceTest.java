package com.lottery.system.service.draw;

import com.lottery.system.entity.Activity;
import com.lottery.system.enums.ActiveStatus;
import com.lottery.system.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class DrawValidationServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private DrawValidationService drawValidationService;
    private Activity activeActivity;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        drawValidationService = new DrawValidationService(redisTemplate);

        // Configure a valid active activity: active status, starts 1 hour ago, ends in
        // 1 hour
        activeActivity = Activity.builder()
                .id(1L)
                .name("Test Activity")
                .status(ActiveStatus.ACTIVE)
                .startTime(OffsetDateTime.now().minusHours(1))
                .endTime(OffsetDateTime.now().plusHours(1))
                .maxDrawsPerUser(3)
                .build();
    }

    @Test
    public void testValidateDrawCount_UnderLimit_Success() {
        Long userId = 100L;
        String redisKey = "user:draw:count:1:100";
        String maxDrawsKey = "activity:1:max_draws";

        // Mock Redis count increment: returns 2 (under maximum limit of 3)
        when(valueOperations.increment(redisKey, 1)).thenReturn(2L);
        // Mock max draws cache hit: returns "3"
        when(valueOperations.get(maxDrawsKey)).thenReturn("3");

        // Should execute cleanly without throwing exceptions
        drawValidationService.validateDrawCount(userId, activeActivity, 1);

        verify(valueOperations, times(1)).increment(redisKey, 1);
        verify(valueOperations, never()).decrement(anyString(), anyLong());
    }

    @Test
    public void testValidateDrawCount_ExceedLimit_ThrowsBusinessException() {
        Long userId = 100L;
        String redisKey = "user:draw:count:1:100";
        String maxDrawsKey = "activity:1:max_draws";

        // Mock Redis count increment: returns 4 (exceeds maximum limit of 3)
        when(valueOperations.increment(redisKey, 1)).thenReturn(4L);
        // Mock max draws cache hit: returns "3"
        when(valueOperations.get(maxDrawsKey)).thenReturn("3");

        // Should throw BusinessException with correct message
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            drawValidationService.validateDrawCount(userId, activeActivity, 1);
        });

        assertTrue(exception.getMessage().contains("Reach rate limit!"));

        // Verify rollback increment: decrement by 1 must be called
        verify(valueOperations, times(1)).decrement(redisKey, 1);
    }
}
