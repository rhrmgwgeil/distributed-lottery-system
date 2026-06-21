package com.lottery.system.service.draw;

import com.lottery.system.entity.Activity;
import com.lottery.system.enums.ActiveStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class DrawValidationService {

    private final StringRedisTemplate redisTemplate;

    public DrawValidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Validates draw count rules using Redis INCR limit.
     * 
     * @param userId    user identifier
     * @param activity  the activity entity containing draw constraints
     * @param drawCount number of draws requested
     */
    public void validateDrawCount(Long userId, Activity activity, int drawCount) {
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new IllegalStateException("Activity has not started or has already ended");
        }

        if (ActiveStatus.ACTIVE != activity.getStatus()) {
            throw new IllegalStateException("Activity is not active");
        }

        String redisKey = "user:draw:count:" + activity.getId() + ":" + userId;
        Long count = redisTemplate.opsForValue().increment(redisKey, drawCount);

        if (count != null && count.equals((long) drawCount)) {
            // Expire key 1 day after activity end to ensure cleanup
            redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
        }

        String maxDrawsKey = "activity:" + activity.getId() + ":max_draws";
        String maxDrawsCached = redisTemplate.opsForValue().get(maxDrawsKey);
        int maxDrawsLimit;
        if (maxDrawsCached != null) {
            maxDrawsLimit = Integer.parseInt(maxDrawsCached);
        } else {
            maxDrawsLimit = activity.getMaxDrawsPerUser();
            redisTemplate.opsForValue().set(maxDrawsKey, String.valueOf(maxDrawsLimit));
        }

        if (count != null && count > maxDrawsLimit) {
            // Roll back the counter increment
            redisTemplate.opsForValue().decrement(redisKey, drawCount);
            throw new IllegalStateException("User has exceeded the maximum draws allowed for this activity");
        }
    }

    /**
     * Compensates draw count by decrementing the counter.
     * 
     * @param userId     user identifier
     * @param activityId activity identifier
     * @param drawCount  number of draws to roll back
     */
    public void rollbackDrawCount(Long userId, Long activityId, int drawCount) {
        String redisKey = "user:draw:count:" + activityId + ":" + userId;
        redisTemplate.opsForValue().decrement(redisKey, drawCount);
    }
}
