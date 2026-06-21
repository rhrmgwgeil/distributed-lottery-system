package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating or updating a prize configuration")
public class PrizeRequestDto {

    @NotNull(message = "Activity ID must not be null")
    @Schema(description = "ID of the associated activity", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activityId;

    @NotBlank(message = "Prize name must not be blank")
    @Schema(description = "Name of the prize", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "Stock must not be null")
    @Min(value = 0, message = "Stock must not be negative")
    @Schema(description = "Stock count for the prize", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;

    @NotNull(message = "Probability must not be null")
    @DecimalMin(value = "0.0000", message = "Probability must be at least 0.0000")
    @DecimalMax(value = "1.0000", message = "Probability must be at most 1.0000")
    @Schema(description = "Winning probability (0.0000 to 1.0000)", example = "0.0500", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal probability;

    @NotNull(message = "Prize type must not be null")
    @Min(value = 1, message = "Prize type must be 1, 2, or 3")
    @Max(value = 3, message = "Prize type must be 1, 2, or 3")
    @Schema(description = "Type of prize (1: Physical, 2: Virtual, 3: None)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer prizeType;
}
