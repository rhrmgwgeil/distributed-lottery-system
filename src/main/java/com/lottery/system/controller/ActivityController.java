package com.lottery.system.controller;

import com.lottery.system.dto.ActivityRequestDto;
import com.lottery.system.dto.ActivityResponseDto;
import com.lottery.system.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities")
@Tag(name = "Activity Controller", description = "Endpoints for activity management")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Create activity", description = "Allows ADMIN to configure a new lottery activity. Operator must have changed default password.")
    public ResponseEntity<ActivityResponseDto> createActivity(@Valid @RequestBody ActivityRequestDto request) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        ActivityResponseDto response = activityService.createActivity(operator, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Update activity", description = "Allows ADMIN to update an existing lottery activity configuration. Operator must have changed default password.")
    public ResponseEntity<ActivityResponseDto> updateActivity(@PathVariable Long id, @Valid @RequestBody ActivityRequestDto request) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        ActivityResponseDto response = activityService.updateActivity(operator, id, request);
        return ResponseEntity.ok(response);
    }
}
