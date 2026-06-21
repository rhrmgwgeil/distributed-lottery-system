package com.lottery.system.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Activity entity representing lucky draw events")
public class Activity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the activity", example = "1")
    private Long id;

    @Column(nullable = false, length = 100)
    @Schema(description = "Name of the activity", example = "Super Summer Lucky Draw")
    private String name;

    @Column(nullable = false, length = 20)
    @Schema(description = "Status of the activity (e.g. ACTIVE, INACTIVE)", example = "ACTIVE")
    private String status;

    @Column(name = "max_draws_per_user", nullable = false)
    @Schema(description = "Maximum number of draws a single user can perform in this activity", example = "5")
    private Integer maxDrawsPerUser;

    @Column(name = "start_time", nullable = false)
    @Schema(description = "Activity start date time")
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    @Schema(description = "Activity end date time")
    private LocalDateTime endTime;
}
