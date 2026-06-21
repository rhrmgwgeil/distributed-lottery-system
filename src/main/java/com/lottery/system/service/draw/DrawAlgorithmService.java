package com.lottery.system.service.draw;

import com.lottery.system.entity.Prize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DrawAlgorithmService {

    /**
     * Randomly selects a prize based on its configured probability.
     * Uses cumulative probability distribution.
     * @param prizes list of all prizes for the activity
     * @return the selected Prize
     */
    public Prize pickPrize(List<Prize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            throw new IllegalStateException("No prizes configured for this activity");
        }

        double randomVal = Math.random() * 100.0; // Range [0.0, 100.0)
        double cumulative = 0.0;
        Prize nonePrize = null;

        for (Prize prize : prizes) {
            if (prize.getPrizeType() == 3) {
                nonePrize = prize;
            }
            cumulative += prize.getProbability().doubleValue();
            if (randomVal < cumulative) {
                return prize;
            }
        }

        // Fallback to "None" type prize if defined
        if (nonePrize != null) {
            return nonePrize;
        }

        // Ultimate fallback to the last prize in the list
        return prizes.get(prizes.size() - 1);
    }
}
