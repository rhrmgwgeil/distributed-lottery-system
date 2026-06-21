package com.lottery.system.service.draw.strategy;

import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;

public interface PrizeProcessStrategy {
    /**
     * Executes the prize processing strategy.
     * @param ticket the current draw ticket
     * @param prize the picked prize
     * @return true if successfully processed/reserved; false if failed or needs downgrade
     */
    boolean execute(DrawTicket ticket, Prize prize);

    /**
     * Returns the type of prize this strategy handles.
     * @return type code: 1 for Physical, 2 for Virtual, 3 for None
     */
    int getPrizeType();
}
