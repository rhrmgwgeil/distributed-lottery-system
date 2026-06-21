package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating or updating a user")
public class UserRequestDto {

    @NotBlank(message = "Username must not be blank")
    @Schema(description = "Username of the user", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Password of the user", example = "secretPass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Scope must not be blank")
    @Schema(description = "Role/Scope of the user (e.g. USER, ADMIN)", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scope;
}
