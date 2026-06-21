package com.lottery.system.service.draw.strategy;

import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import com.lottery.system.enums.TicketStatus;
import org.springframework.stereotype.Component;

@Component
public class NonePrizeStrategy implements PrizeProcessStrategy {

    @Override
    public boolean execute(DrawTicket ticket, Prize prize) {
        // Direct update ticket to no-prize status
        ticket.setPrizeId(prize.getId());
        ticket.setStatus(TicketStatus.NO_PRIZE);
        return true;
    }

    @Override
    public int getPrizeType() {
        return 3;
    }
}
