package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object to participate in a lucky draw activity")
public class DrawRequest {

    @NotNull(message = "Activity ID must not be null")
    @Schema(description = "ID of the lottery activity to draw from", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activityId;

    @Min(value = 1, message = "Draw count must be at least 1")
    @Max(value = 50, message = "Draw count cannot exceed 50")
    @Schema(description = "Number of draws to perform in this request", example = "5")
    private Integer count = 1;
}
