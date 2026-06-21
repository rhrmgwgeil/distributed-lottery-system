package com.lottery.system.controller;

import com.lottery.system.dto.LoginRequest;
import com.lottery.system.dto.LoginResponse;
import com.lottery.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication Controller", description = "Endpoints for login and logout operations")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT token", description = "Verifies username and password, then returns a JWT token.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate token", description = "Places the current JWT token into a Redis blocklist.")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        authService.logout(bearerToken);
        return ResponseEntity.ok("Successfully logged out");
    }
}
