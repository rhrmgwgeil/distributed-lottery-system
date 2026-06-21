package com.lottery.system.service;

import com.lottery.system.dto.ChangePasswordRequest;
import com.lottery.system.dto.UserRequestDto;
import com.lottery.system.dto.UserResponseDto;

import java.util.List;

public interface UserService {
    /**
     * Changes current user's password and sets isPasswordChanged to true.
     * @param currentUsername the current user's username
     * @param request containing old and new passwords
     */
    void changePassword(String currentUsername, ChangePasswordRequest request);

    /**
     * Creates a new user profile.
     * @param operatorUsername the username of the calling administrator
     * @param request containing details of the user to be created
     * @return DTO representation of the created user
     */
    UserResponseDto createUser(String operatorUsername, UserRequestDto request);

    /**
     * Lists all user profiles.
     * @param operatorUsername the username of the calling administrator
     * @return list of DTO representations of all users
     */
    List<UserResponseDto> getAllUsers(String operatorUsername);

    /**
     * Retrieves specific user details by ID.
     * @param operatorUsername the username of the calling administrator
     * @param id target user's identification number
     * @return DTO representation of the target user
     */
    UserResponseDto getUserById(String operatorUsername, Long id);

    /**
     * Updates an existing user's details.
     * @param operatorUsername the username of the calling administrator
     * @param id target user's identification number
     * @param request containing updated details of the user
     * @return DTO representation of the updated user
     */
    UserResponseDto updateUser(String operatorUsername, Long id, UserRequestDto request);

    /**
     * Deletes a user profile.
     * @param operatorUsername the username of the calling administrator
     * @param id target user's identification number
     */
    void deleteUser(String operatorUsername, Long id);
}
