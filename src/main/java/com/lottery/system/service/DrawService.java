package com.lottery.system.service;

import com.lottery.system.dto.DrawRequest;
import com.lottery.system.dto.DrawTicketDto;
import java.util.List;

public interface DrawService {
    /**
     * Resolves the user, performs the lottery draw, and returns the tickets.
     * @param username the username of the participating user
     * @param request containing the target activity ID and count
     * @return DTO representation of the draw tickets
     */
    List<DrawTicketDto> performDraw(String username, DrawRequest request);

    /**
     * Retrieves the status of a specific draw ticket by ID.
     * @param ticketId target ticket UUID
     * @return DTO representation of the draw ticket
     */
    DrawTicketDto getDrawResult(String ticketId);
}
