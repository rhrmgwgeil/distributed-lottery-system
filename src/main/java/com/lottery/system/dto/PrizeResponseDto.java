package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response representation of a prize configuration")
public class PrizeResponseDto {

    @Schema(description = "Unique ID of the prize", example = "10")
    private Long id;

    @Schema(description = "ID of the associated activity", example = "1")
    private Long activityId;

    @Schema(description = "Name of the prize", example = "iPhone 15 Pro")
    private String name;

    @Schema(description = "Stock count for the prize", example = "100")
    private Integer stock;

    @Schema(description = "Winning probability percentage (1 to 100)", example = "5.0000")
    private BigDecimal probability;

    @Schema(description = "Type of prize (1: Physical, 2: Virtual, 3: None)", example = "1")
    private Integer prizeType;
}
