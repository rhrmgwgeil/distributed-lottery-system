package com.lottery.system.service.impl;

import com.lottery.system.dto.DrawRequest;
import com.lottery.system.dto.DrawTicketDto;
import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.User;
import com.lottery.system.repository.DrawTicketRepository;
import com.lottery.system.repository.UserRepository;
import com.lottery.system.service.DrawService;
import com.lottery.system.service.draw.DrawAppService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DrawServiceImpl implements DrawService {

    private final DrawAppService drawAppService;
    private final UserRepository userRepository;
    private final DrawTicketRepository drawTicketRepository;

    public DrawServiceImpl(DrawAppService drawAppService,
                           UserRepository userRepository,
                           DrawTicketRepository drawTicketRepository) {
        this.drawAppService = drawAppService;
        this.userRepository = userRepository;
        this.drawTicketRepository = drawTicketRepository;
    }

    @Override
    public DrawTicketDto performDraw(String username, DrawRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        DrawTicket ticket = drawAppService.performDraw(user.getId(), request.getActivityId());
        return mapToDto(ticket);
    }

    @Override
    public DrawTicketDto getDrawResult(String ticketId) {
        DrawTicket ticket = drawTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draw ticket not found"));

        return mapToDto(ticket);
    }

    private DrawTicketDto mapToDto(DrawTicket ticket) {
        return DrawTicketDto.builder()
                .ticketId(ticket.getTicketId())
                .activityId(ticket.getActivityId())
                .userId(ticket.getUserId())
                .status(ticket.getStatus())
                .prizeId(ticket.getPrizeId())
                .build();
    }
}
