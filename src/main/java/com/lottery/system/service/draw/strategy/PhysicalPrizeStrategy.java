package com.lottery.system.service.draw.strategy;

import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PhysicalPrizeStrategy implements PrizeProcessStrategy {

    private final StringRedisTemplate redisTemplate;

    public PhysicalPrizeStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean execute(DrawTicket ticket, Prize prize) {
        String redisKey = "prize:stock:" + prize.getId();
        Long stock = redisTemplate.opsForValue().decrement(redisKey);

        if (stock == null || stock < 0) {
            // Revert the decrement to maintain correct stock count (avoid runaway negatives)
            redisTemplate.opsForValue().increment(redisKey);
            return false; // Triggers downgrade in the calling service
        }

        // Successfully reserved stock in Redis
        ticket.setPrizeId(prize.getId());
        return true;
    }

    @Override
    public int getPrizeType() {
        return 1;
    }
}
