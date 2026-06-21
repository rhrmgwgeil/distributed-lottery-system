package com.lottery.system.service;

import com.lottery.system.dto.PrizeRequestDto;
import com.lottery.system.dto.PrizeResponseDto;

public interface PrizeService {
    /**
     * Creates a new prize configuration.
     * If the prize type is physical (1), initializes its stock in Redis.
     * @param operatorUsername the username of the calling administrator
     * @param request containing details of the new prize
     * @return DTO representation of the created prize
     */
    PrizeResponseDto createPrize(String operatorUsername, PrizeRequestDto request);

    /**
     * Updates an existing prize configuration.
     * If the prize type is physical (1), synchronizes the updated stock count to Redis.
     * @param operatorUsername the username of the calling administrator
     * @param id target prize ID
     * @param request containing updated details of the prize
     * @return DTO representation of the updated prize
     */
    PrizeResponseDto updatePrize(String operatorUsername, Long id, PrizeRequestDto request);
}
