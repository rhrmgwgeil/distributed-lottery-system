package com.lottery.system.service;

import com.lottery.system.dto.ActivityRequestDto;
import com.lottery.system.dto.ActivityResponseDto;

public interface ActivityService {
    /**
     * Creates a new lottery activity event.
     * @param operatorUsername the username of the calling administrator
     * @param request containing new activity details
     * @return DTO representation of the created activity
     */
    ActivityResponseDto createActivity(String operatorUsername, ActivityRequestDto request);

    /**
     * Updates an existing lottery activity event.
     * @param operatorUsername the username of the calling administrator
     * @param id target activity ID
     * @param request containing updated activity details
     * @return DTO representation of the updated activity
     */
    ActivityResponseDto updateActivity(String operatorUsername, Long id, ActivityRequestDto request);
}
