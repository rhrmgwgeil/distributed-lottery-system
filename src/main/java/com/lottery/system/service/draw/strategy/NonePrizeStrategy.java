package com.lottery.system.service.draw.strategy;

import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import org.springframework.stereotype.Component;

@Component
public class NonePrizeStrategy implements PrizeProcessStrategy {

    @Override
    public boolean execute(DrawTicket ticket, Prize prize) {
        // Direct update ticket to failed status (none prize)
        ticket.setPrizeId(prize.getId());
        ticket.setStatus(2); // 2: FAILED (Thanks for playing)
        return true;
    }

    @Override
    public int getPrizeType() {
        return 3;
    }
}
