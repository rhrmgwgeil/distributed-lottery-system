package com.lottery.system.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Prize entity detailing award types, stocks, and winning probabilities")
public class Prize extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the prize", example = "10")
    private Long id;

    @Column(name = "activity_id", nullable = false)
    @Schema(description = "ID of the parent activity", example = "1")
    private Long activityId;

    @Column(nullable = false, length = 100)
    @Schema(description = "Name of the prize", example = "iPhone 15 Pro")
    private String name;

    @Column(nullable = false)
    @Schema(description = "Remaining stock count in database", example = "100")
    private Integer stock;

    @Column(nullable = false, precision = 5, scale = 4)
    @Schema(description = "Winning probability between 0.0000 and 1.0000", example = "0.0500")
    private BigDecimal probability;

    @Column(name = "prize_type", nullable = false)
    @Schema(description = "Type of prize (1: Physical, 2: Virtual, 3: None/Thanks)", example = "1")
    private Integer prizeType;

    @Version
    @Schema(description = "Version field for optimistic locking")
    private Long version;
}
