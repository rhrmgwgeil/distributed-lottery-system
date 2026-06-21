package com.lottery.system.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User entity for security and authentication")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the user", example = "1")
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    @Schema(description = "Unique username of the user", example = "admin")
    private String username;

    @Column(nullable = false)
    @Schema(description = "BCrypt encrypted password", example = "$2a$10$...")
    private String password;

    @Column(nullable = false, length = 20)
    @Schema(description = "Scope or authority level of the user", example = "ADMIN")
    private String scope;

    @Column(name = "is_password_changed", nullable = false)
    @Builder.Default
    @Schema(description = "Flag indicating if the user has changed the default password", example = "true")
    private boolean isPasswordChanged = false;
}
