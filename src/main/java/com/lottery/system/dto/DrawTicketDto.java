package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO representing the status of a draw ticket")
public class DrawTicketDto {

    @Schema(description = "Unique UUID of the draw ticket", example = "a2b3c4d5-...")
    private String ticketId;

    @Schema(description = "ID of the lottery activity", example = "1")
    private Long activityId;

    @Schema(description = "ID of the user who performed the draw", example = "5")
    private Long userId;

    @Schema(description = "Status of the draw ticket (0: INIT, 1: SUCCESS, 2: FAILED)", example = "0")
    private Integer status;

    @Schema(description = "ID of the won prize (null if none or pending)", example = "10")
    private Long prizeId;
}
