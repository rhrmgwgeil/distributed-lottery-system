package com.lottery.system.service;

import com.lottery.system.dto.DrawRequest;
import com.lottery.system.dto.DrawTicketDto;

public interface DrawService {
    /**
     * Resolves the user, performs the lottery draw, and returns the ticket.
     * @param username the username of the participating user
     * @param request containing the target activity ID
     * @return DTO representation of the draw ticket
     */
    DrawTicketDto performDraw(String username, DrawRequest request);

    /**
     * Retrieves the status of a specific draw ticket by ID.
     * @param ticketId target ticket UUID
     * @return DTO representation of the draw ticket
     */
    DrawTicketDto getDrawResult(String ticketId);
}
