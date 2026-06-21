package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
}
