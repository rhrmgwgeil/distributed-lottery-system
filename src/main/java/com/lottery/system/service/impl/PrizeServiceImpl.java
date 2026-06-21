package com.lottery.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.system.dto.PrizeRequestDto;
import com.lottery.system.dto.PrizeResponseDto;
import com.lottery.system.entity.Prize;
import com.lottery.system.entity.User;
import com.lottery.system.repository.PrizeRepository;
import com.lottery.system.repository.UserRepository;
import com.lottery.system.service.PrizeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PrizeServiceImpl implements PrizeService {

    private final PrizeRepository prizeRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PrizeServiceImpl(PrizeRepository prizeRepository,
                            UserRepository userRepository,
                            StringRedisTemplate redisTemplate,
                            ObjectMapper objectMapper) {
        this.prizeRepository = prizeRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PrizeResponseDto createPrize(String operatorUsername, PrizeRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        // Probability sum validation (excludePrizeId is null for new prize)
        validateProbabilitySum(request.getActivityId(), request.getProbability(), null);

        Prize prize = Prize.builder()
                .activityId(request.getActivityId())
                .name(request.getName())
                .stock(request.getStock())
                .probability(request.getProbability())
                .prizeType(request.getPrizeType())
                .build();

        Prize saved = prizeRepository.save(prize);

        // Sync stock & list to Redis
        syncPrizeToRedis(saved);

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public PrizeResponseDto updatePrize(String operatorUsername, Long id, PrizeRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        // Probability sum validation (exclude current prize ID)
        validateProbabilitySum(request.getActivityId(), request.getProbability(), id);

        Prize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prize not found"));

        prize.setActivityId(request.getActivityId());
        prize.setName(request.getName());
        prize.setStock(request.getStock());
        prize.setProbability(request.getProbability());
        prize.setPrizeType(request.getPrizeType());

        Prize saved = prizeRepository.save(prize);

        // Sync stock & list to Redis
        syncPrizeToRedis(saved);

        return mapToDto(saved);
    }

    private void validateProbabilitySum(Long activityId, BigDecimal newProbability, Long excludePrizeId) {
        List<Prize> existingPrizes = prizeRepository.findByActivityId(activityId);
        BigDecimal sum = newProbability;

        for (Prize p : existingPrizes) {
            if (excludePrizeId != null && p.getId().equals(excludePrizeId)) {
                continue; // Skip the prize currently being updated
            }
            sum = sum.add(p.getProbability());
        }

        if (sum.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "Total probability of prizes for activity " + activityId + " cannot exceed 1.0 (100%). Current sum is " + sum
            );
        }
    }

    private void syncPrizeToRedis(Prize saved) {
        // 1. If physical (1) or virtual (2), sync stock to Redis: prize:{id}:stock
        if (saved.getPrizeType() == 1 || saved.getPrizeType() == 2) {
            String stockKey = "prize:" + saved.getId() + ":stock";
            redisTemplate.opsForValue().set(stockKey, String.valueOf(saved.getStock()));
        }

        // 2. Fetch all latest prizes, serialize to JSON, and sync to Redis: activity:{activityId}:prizes
        List<Prize> latestPrizes = prizeRepository.findByActivityId(saved.getActivityId());
        String prizesKey = "activity:" + saved.getActivityId() + ":prizes";
        try {
            String prizesJson = objectMapper.writeValueAsString(latestPrizes);
            redisTemplate.opsForValue().set(prizesKey, prizesJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize and sync prizes to Redis for activity " + saved.getActivityId(), e);
        }
    }

    private void verifyOperatorPasswordChanged(String operatorUsername) {
        User operator = userRepository.findByUsername(operatorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operator user context not found"));
        if (!operator.isPasswordChanged()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please change your default password first before administrative actions");
        }
    }

    private PrizeResponseDto mapToDto(Prize prize) {
        return PrizeResponseDto.builder()
                .id(prize.getId())
                .activityId(prize.getActivityId())
                .name(prize.getName())
                .stock(prize.getStock())
                .probability(prize.getProbability())
                .prizeType(prize.getPrizeType())
                .build();
    }
}
