package com.lottery.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object to change password")
public class ChangePasswordRequest {

    @NotBlank(message = "Old password must not be blank")
    @Schema(description = "Existing password", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "New password must not be blank")
    @Schema(description = "Desired new password", example = "newAdminPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
