package com.lottery.system.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "user_activity_counters",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "activity_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Draw counter tracking how many times a user has participated in an activity")
public class UserActivityCounter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the counter", example = "100")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Schema(description = "ID of the user", example = "5")
    private Long userId;

    @Column(name = "activity_id", nullable = false)
    @Schema(description = "ID of the activity", example = "1")
    private Long activityId;

    @Column(name = "current_draw_count", nullable = false)
    @Schema(description = "Current participation count of the user", example = "3")
    private Integer currentDrawCount;

    @Version
    @Schema(description = "Version field for optimistic locking")
    private Long version;
}
