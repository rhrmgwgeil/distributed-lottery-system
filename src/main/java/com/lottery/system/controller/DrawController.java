package com.lottery.system.controller;

import com.lottery.system.dto.DrawRequest;
import com.lottery.system.dto.DrawTicketDto;
import com.lottery.system.service.DrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/draws")
@Tag(name = "Draw Controller", description = "Endpoints for initiating draws and checking results")
public class DrawController {

    private final DrawService drawService;

    public DrawController(DrawService drawService) {
        this.drawService = drawService;
    }

    @PostMapping
    @Operation(summary = "Initiate a draw ticket", description = "Performs initial validation, picks a prize, reserves Redis stock, and pushes to MQ. Returns HTTP 202 Accepted for async processing.")
    public ResponseEntity<List<DrawTicketDto>> performDraw(@Valid @RequestBody DrawRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<DrawTicketDto> response = drawService.performDraw(username, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{ticketId}/result")
    @Operation(summary = "Get draw result", description = "Queries database for ticket state (INIT, SUCCESS, or FAILED).")
    public ResponseEntity<DrawTicketDto> getDrawResult(@PathVariable String ticketId) {
        DrawTicketDto response = drawService.getDrawResult(ticketId);
        return ResponseEntity.ok(response);
    }
}
