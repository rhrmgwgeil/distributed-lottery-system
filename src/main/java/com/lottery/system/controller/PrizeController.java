package com.lottery.system.controller;

import com.lottery.system.dto.PrizeRequestDto;
import com.lottery.system.dto.PrizeResponseDto;
import com.lottery.system.service.PrizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prizes")
@Tag(name = "Prize Controller", description = "Endpoints for prize configuration management")
public class PrizeController {

    private final PrizeService prizeService;

    public PrizeController(PrizeService prizeService) {
        this.prizeService = prizeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Create prize", description = "Allows ADMIN to configure a new prize. Operator must have changed default password. Physical prizes will initialize Redis stock.")
    public ResponseEntity<PrizeResponseDto> createPrize(@Valid @RequestBody PrizeRequestDto request) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        PrizeResponseDto response = prizeService.createPrize(operator, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Update prize", description = "Allows ADMIN to update prize configuration. Operator must have changed default password. Physical prizes will synchronize updated stock to Redis.")
    public ResponseEntity<PrizeResponseDto> updatePrize(@PathVariable Long id, @Valid @RequestBody PrizeRequestDto request) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        PrizeResponseDto response = prizeService.updatePrize(operator, id, request);
        return ResponseEntity.ok(response);
    }
}
