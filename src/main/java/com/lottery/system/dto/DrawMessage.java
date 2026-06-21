package com.lottery.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ticketId;
    private Long userId;
    private Long activityId;
    private Long prizeId;
}
