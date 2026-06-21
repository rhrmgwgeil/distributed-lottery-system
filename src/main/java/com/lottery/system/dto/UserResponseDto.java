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
@Schema(description = "Response representation of user details")
public class UserResponseDto {

    @Schema(description = "User identification number", example = "5")
    private Long id;

    @Schema(description = "Username of the user", example = "john_doe")
    private String username;

    @Schema(description = "Scope / authority of the user", example = "USER")
    private String scope;

    @Schema(description = "Indicates whether the user has modified their default password", example = "true")
    private boolean isPasswordChanged;
}
