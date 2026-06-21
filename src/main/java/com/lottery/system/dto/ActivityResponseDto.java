package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.lottery.system.enums.ActiveStatus;

import java.time.OffsetDateTime;

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
    private ActiveStatus status;

    @Schema(description = "Maximum draw attempts allowed per user", example = "5")
    private Integer maxDrawsPerUser;

    @Schema(description = "Activity start date and time")
    private OffsetDateTime startTime;

    @Schema(description = "Activity end date and time")
    private OffsetDateTime endTime;
}
