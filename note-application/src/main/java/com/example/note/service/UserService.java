package com.example.note.service;

import com.example.note.model.dto.UserDto;
import com.example.note.exception.ResourceNotFoundException;
import com.example.note.exception.TokenHasExpiredException;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public interface UserService {

    UserDto createUser(UserDto userDto, HttpServletRequest request);

    /** Verify email with token from registration email */
    void verifyEmail(String token) throws IllegalArgumentException, TokenHasExpiredException;

    List<UserDto> getAllUsers();

    UserDto getUserById(Long idUser) throws ResourceNotFoundException;

    UserDto updateUser(UserDto userDto, Long idUser) throws ResourceNotFoundException;

    void deleteUser(Long idUser) throws ResourceNotFoundException;

    void uploadUserProfile(MultipartFile file) throws IOException;

    byte[] getCurrentUserPhotoProfile() throws IOException, ResourceNotFoundException;

    void deleteUserProfile() throws ResourceNotFoundException;

    /**
     * Forgot password — always succeeds from caller's view (anti email enumeration).
     * Publishes reset email only if the email exists.
     */
    void processForgotPasswordRequest(String email, HttpServletRequest request);

    void resetTokenValidation(String token) throws IllegalArgumentException, TokenHasExpiredException;

    void updateNewPassword(String password, String token) throws TokenHasExpiredException;
}
