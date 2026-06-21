package com.lottery.system.controller;

import com.lottery.system.dto.ChangePasswordRequest;
import com.lottery.system.dto.UserRequestDto;
import com.lottery.system.dto.UserResponseDto;
import com.lottery.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Controller", description = "Endpoints for user profile and administration")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for logged-in user", description = "Allows an authenticated user to change their password and flags is_password_changed as true.")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username, request);
        return ResponseEntity.ok("Password successfully updated");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Create user", description = "Allows ADMIN to create a new user. Operator must have changed default password.")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponseDto response = userService.createUser(operator, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "List all users", description = "Allows ADMIN to retrieve all user profiles. Operator must have changed default password.")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        List<UserResponseDto> response = userService.getAllUsers(operator);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Get user by ID", description = "Allows ADMIN to retrieve specific user profile. Operator must have changed default password.")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponseDto response = userService.getUserById(operator, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Update user details", description = "Allows ADMIN to modify user profile details. Operator must have changed default password.")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto request) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponseDto response = userService.updateUser(operator, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Delete user", description = "Allows ADMIN to delete a user profile. Operator must have changed default password.")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.deleteUser(operator, id);
        return ResponseEntity.noContent().build();
    }
}
