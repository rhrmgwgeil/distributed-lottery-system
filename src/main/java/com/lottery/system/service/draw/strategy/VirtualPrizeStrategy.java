package com.lottery.system.service.draw.strategy;

import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import org.springframework.stereotype.Component;

@Component
public class VirtualPrizeStrategy implements PrizeProcessStrategy {

    @Override
    public boolean execute(DrawTicket ticket, Prize prize) {
        // Virtual prizes succeed immediately without Redis stock decrement
        ticket.setPrizeId(prize.getId());
        return true;
    }

    @Override
    public int getPrizeType() {
        return 2;
    }
}
