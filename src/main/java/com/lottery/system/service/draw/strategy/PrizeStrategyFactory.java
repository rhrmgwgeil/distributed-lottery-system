package com.lottery.system.service.draw.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PrizeStrategyFactory {

    private final Map<Integer, PrizeProcessStrategy> strategyMap;

    public PrizeStrategyFactory(List<PrizeProcessStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(PrizeProcessStrategy::getPrizeType, Function.identity()));
    }

    public PrizeProcessStrategy getStrategy(int prizeType) {
        PrizeProcessStrategy strategy = strategyMap.get(prizeType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported prize type: " + prizeType);
        }
        return strategy;
    }
}
