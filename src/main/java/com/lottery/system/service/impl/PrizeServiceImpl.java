package com.lottery.system.service.impl;

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
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrizeServiceImpl implements PrizeService {

    private final PrizeRepository prizeRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    public PrizeServiceImpl(PrizeRepository prizeRepository,
                            UserRepository userRepository,
                            StringRedisTemplate redisTemplate) {
        this.prizeRepository = prizeRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public PrizeResponseDto createPrize(String operatorUsername, PrizeRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        Prize prize = Prize.builder()
                .activityId(request.getActivityId())
                .name(request.getName())
                .stock(request.getStock())
                .probability(request.getProbability())
                .prizeType(request.getPrizeType())
                .build();

        Prize saved = prizeRepository.save(prize);

        // Sync Redis stock if it's a physical prize
        if (saved.getPrizeType() == 1) {
            String redisKey = "prize:stock:" + saved.getId();
            redisTemplate.opsForValue().set(redisKey, String.valueOf(saved.getStock()));
        }

        return mapToDto(saved);
    }

    @Override
    public PrizeResponseDto updatePrize(String operatorUsername, Long id, PrizeRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        Prize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prize not found"));

        prize.setActivityId(request.getActivityId());
        prize.setName(request.getName());
        prize.setStock(request.getStock());
        prize.setProbability(request.getProbability());
        prize.setPrizeType(request.getPrizeType());

        Prize saved = prizeRepository.save(prize);

        // Sync Redis stock if it's a physical prize
        if (saved.getPrizeType() == 1) {
            String redisKey = "prize:stock:" + saved.getId();
            redisTemplate.opsForValue().set(redisKey, String.valueOf(saved.getStock()));
        }

        return mapToDto(saved);
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
