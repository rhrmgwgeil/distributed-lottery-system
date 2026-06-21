package com.lottery.system.service;

import com.lottery.system.dto.LoginRequest;
import com.lottery.system.dto.LoginResponse;

public interface AuthService {
    /**
     * Authenticates a user and generates a JWT.
     * @param loginRequest containing username and password
     * @return login response containing the token
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * Invalidate the current session's JWT by adding it to Redis blocklist.
     * @param bearerToken authorization header string
     */
    void logout(String bearerToken);
}
