package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating or updating a lottery activity")
public class ActivityRequestDto {

    @NotBlank(message = "Activity name must not be blank")
    @Schema(description = "Name of the lucky draw activity", example = "Mid-Autumn Lucky Spin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Activity status must not be blank")
    @Schema(description = "Status of the activity (e.g. ACTIVE, INACTIVE)", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @NotNull(message = "Max draws per user must not be null")
    @Min(value = 1, message = "Max draws per user must be at least 1")
    @Schema(description = "Maximum draw attempts allowed per user", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer maxDrawsPerUser;

    @NotNull(message = "Start time must not be null")
    @Schema(description = "Activity start date and time", example = "2026-09-15T00:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @NotNull(message = "End time must not be null")
    @Schema(description = "Activity end date and time", example = "2026-09-30T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;
}
