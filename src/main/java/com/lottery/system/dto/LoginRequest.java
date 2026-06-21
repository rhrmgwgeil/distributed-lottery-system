package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for login authentication")
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Schema(description = "Username of the user", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Password of the user", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
