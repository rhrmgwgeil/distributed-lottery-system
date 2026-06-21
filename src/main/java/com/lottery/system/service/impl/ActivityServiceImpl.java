package com.lottery.system.service.impl;

import com.lottery.system.dto.ActivityRequestDto;
import com.lottery.system.dto.ActivityResponseDto;
import com.lottery.system.entity.Activity;
import com.lottery.system.entity.User;
import com.lottery.system.repository.ActivityRepository;
import com.lottery.system.repository.UserRepository;
import com.lottery.system.service.ActivityService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    public ActivityServiceImpl(ActivityRepository activityRepository,
                               UserRepository userRepository,
                               StringRedisTemplate redisTemplate) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public ActivityResponseDto createActivity(String operatorUsername, ActivityRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        if (request.getStartTime() != null && request.getEndTime() != null && 
            request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Activity start time cannot be after end time");
        }

        Activity activity = Activity.builder()
                .name(request.getName())
                .status(request.getStatus())
                .maxDrawsPerUser(request.getMaxDrawsPerUser())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        Activity saved = activityRepository.save(activity);

        // Sync max draws count to Redis: activity:{id}:max_draws
        String maxDrawsKey = "activity:" + saved.getId() + ":max_draws";
        redisTemplate.opsForValue().set(maxDrawsKey, String.valueOf(saved.getMaxDrawsPerUser()));

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ActivityResponseDto updateActivity(String operatorUsername, Long id, ActivityRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        if (request.getStartTime() != null && request.getEndTime() != null && 
            request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Activity start time cannot be after end time");
        }

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        activity.setName(request.getName());
        activity.setStatus(request.getStatus());
        activity.setMaxDrawsPerUser(request.getMaxDrawsPerUser());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());

        Activity saved = activityRepository.save(activity);

        // Sync max draws count to Redis: activity:{id}:max_draws
        String maxDrawsKey = "activity:" + saved.getId() + ":max_draws";
        redisTemplate.opsForValue().set(maxDrawsKey, String.valueOf(saved.getMaxDrawsPerUser()));

        return mapToDto(saved);
    }

    private void verifyOperatorPasswordChanged(String operatorUsername) {
        User operator = userRepository.findByUsername(operatorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operator user context not found"));
        if (!operator.isPasswordChanged()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please change your default password first before administrative actions");
        }
    }

    private ActivityResponseDto mapToDto(Activity activity) {
        return ActivityResponseDto.builder()
                .id(activity.getId())
                .name(activity.getName())
                .status(activity.getStatus())
                .maxDrawsPerUser(activity.getMaxDrawsPerUser())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .build();
    }
}
