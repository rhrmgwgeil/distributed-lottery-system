package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing JWT token after successful authentication")
public class LoginResponse {

    @Schema(description = "Access token generated for authenticated sessions", example = "eyJhbGciOi...")
    private String token;
}
