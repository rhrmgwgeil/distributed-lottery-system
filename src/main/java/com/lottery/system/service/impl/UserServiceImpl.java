package com.lottery.system.service.impl;

import com.lottery.system.dto.ChangePasswordRequest;
import com.lottery.system.dto.UserRequestDto;
import com.lottery.system.dto.UserResponseDto;
import com.lottery.system.entity.User;
import com.lottery.system.repository.UserRepository;
import com.lottery.system.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void changePassword(String currentUsername, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChanged(true);
        userRepository.save(user);
    }

    @Override
    public UserResponseDto createUser(String operatorUsername, UserRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .scope(request.getScope())
                .isPasswordChanged(false)
                .build();

        User saved = userRepository.save(newUser);
        return mapToResponseDto(saved);
    }

    @Override
    public List<UserResponseDto> getAllUsers(String operatorUsername) {
        verifyOperatorPasswordChanged(operatorUsername);

        return userRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDto getUserById(String operatorUsername, Long id) {
        verifyOperatorPasswordChanged(operatorUsername);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return mapToResponseDto(user);
    }

    @Override
    public UserResponseDto updateUser(String operatorUsername, Long id, UserRequestDto request) {
        verifyOperatorPasswordChanged(operatorUsername);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setScope(request.getScope());
        User saved = userRepository.save(user);

        return mapToResponseDto(saved);
    }

    @Override
    public void deleteUser(String operatorUsername, Long id) {
        verifyOperatorPasswordChanged(operatorUsername);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        userRepository.delete(user);
    }

    private void verifyOperatorPasswordChanged(String operatorUsername) {
        User operator = userRepository.findByUsername(operatorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operator user context not found"));
        if (!operator.isPasswordChanged()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please change your default password first before administrative actions");
        }
    }

    private UserResponseDto mapToResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .scope(user.getScope())
                .isPasswordChanged(user.isPasswordChanged())
                .build();
    }
}
