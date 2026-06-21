package com.lottery.system.service.impl;

import com.lottery.system.config.JwtTokenProvider;
import com.lottery.system.dto.LoginRequest;
import com.lottery.system.dto.LoginResponse;
import com.lottery.system.service.AuthService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            StringRedisTemplate redisTemplate) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        String username = authentication.getName();
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("SCOPE_") ? auth.substring(6) : auth)
                .findFirst()
                .orElse("USER");

        String jwt = tokenProvider.generateToken(username, scope);
        return new LoginResponse(jwt);
    }

    @Override
    public void logout(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String jwt = bearerToken.substring(7);
            long ttlMs = tokenProvider.getRemainingTtlMs(jwt);
            if (ttlMs > 0) {
                String logoutKey = "logout:" + jwt;
                redisTemplate.opsForValue().set(logoutKey, "true", ttlMs, TimeUnit.MILLISECONDS);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header");
        }
    }
}
