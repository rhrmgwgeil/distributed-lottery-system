package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response representation of a lottery activity")
public class ActivityResponseDto {

    @Schema(description = "Unique ID of the activity", example = "1")
    private Long id;

    @Schema(description = "Name of the lucky draw activity", example = "Mid-Autumn Lucky Spin")
    private String name;

    @Schema(description = "Status of the activity", example = "ACTIVE")
    private String status;

    @Schema(description = "Maximum draw attempts allowed per user", example = "5")
    private Integer maxDrawsPerUser;

    @Schema(description = "Activity start date and time")
    private LocalDateTime startTime;

    @Schema(description = "Activity end date and time")
    private LocalDateTime endTime;
}
