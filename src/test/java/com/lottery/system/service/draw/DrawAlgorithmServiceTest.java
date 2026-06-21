package com.lottery.system.service.draw;

import com.lottery.system.entity.Prize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DrawAlgorithmServiceTest {

    private DrawAlgorithmService drawAlgorithmService;

    @BeforeEach
    public void setUp() {
        drawAlgorithmService = new DrawAlgorithmService();
    }

    @Test
    public void testProbabilityDistribution() {
        Prize physicalPrize = Prize.builder().id(1L).prizeType(1).probability(new BigDecimal("0.10")).name("Physical").build();
        Prize virtualPrize = Prize.builder().id(2L).prizeType(2).probability(new BigDecimal("0.20")).name("Virtual").build();
        Prize nonePrize = Prize.builder().id(3L).prizeType(3).probability(new BigDecimal("0.70")).name("None").build();

        List<Prize> prizes = Arrays.asList(physicalPrize, virtualPrize, nonePrize);

        int simulations = 10000;
        Map<Long, Integer> counts = new HashMap<>();
        counts.put(1L, 0);
        counts.put(2L, 0);
        counts.put(3L, 0);

        for (int i = 0; i < simulations; i++) {
            Prize picked = drawAlgorithmService.pickPrize(prizes);
            counts.put(picked.getId(), counts.get(picked.getId()) + 1);
        }

        double physicalRate = (double) counts.get(1L) / simulations;
        double virtualRate = (double) counts.get(2L) / simulations;
        double noneRate = (double) counts.get(3L) / simulations;

        // Print results to console for verification
        System.out.println("=== Probability Distribution Simulation Results (N=" + simulations + ") ===");
        System.out.println(String.format("Physical Prize Rate : %.4f (Expected: 0.1000, Diff: %.4f)", physicalRate, (physicalRate - 0.10)));
        System.out.println(String.format("Virtual Prize Rate  : %.4f (Expected: 0.2000, Diff: %.4f)", virtualRate, (virtualRate - 0.20)));
        System.out.println(String.format("None/Thanks Rate    : %.4f (Expected: 0.7000, Diff: %.4f)", noneRate, (noneRate - 0.70)));
        System.out.println("=======================================================================");

        // Verify with a margin of error (e.g. +/- 2.5%)
        double margin = 0.025;
        assertTrue(Math.abs(physicalRate - 0.10) < margin, "Physical rate " + physicalRate + " is out of bounds");
        assertTrue(Math.abs(virtualRate - 0.20) < margin, "Virtual rate " + virtualRate + " is out of bounds");
        assertTrue(Math.abs(noneRate - 0.70) < margin, "None rate " + noneRate + " is out of bounds");
    }
}
