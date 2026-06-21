package com.lottery.system.entity;

import com.lottery.system.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "draw_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Draw ticket entity capturing the state of a user's lottery attempt")
public class DrawTicket extends BaseEntity {

    @Id
    @Column(name = "ticket_id", length = 36, nullable = false)
    @Schema(description = "Unique UUID of the draw ticket", example = "a2b3c4d5-...")
    private String ticketId;

    @Column(name = "activity_id", nullable = false)
    @Schema(description = "ID of the associated activity", example = "1")
    private Long activityId;

    @Column(name = "user_id", nullable = false)
    @Schema(description = "ID of the participating user", example = "5")
    private Long userId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Status of the draw ticket", example = "INIT")
    private TicketStatus status;

    @Column(name = "prize_id")
    @Schema(description = "ID of the won prize (null if none/pending)", example = "10")
    private Long prizeId;
}
