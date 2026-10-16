package com.example.note.controller;

import com.example.note.model.dto.UserDto;
import com.example.note.exception.ResourceNotFoundException;
import com.example.note.exception.TokenHasExpiredException;
import com.example.note.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(
            @Valid @RequestBody UserDto userDto,
            HttpServletRequest request
    ) {
        userDto.setRole("USER");
        UserDto created = userService.createUser(userDto, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** Public: verify email from registration link */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token)
            throws IllegalArgumentException, TokenHasExpiredException {
        userService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully. You can now login.");
    }

    /** Admin only: list all users */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** Admin or the user themselves */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isSelf(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) throws ResourceNotFoundException {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /** Admin or the user themselves (update own profile) */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isSelf(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserDto userDto, @PathVariable Long id)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(userService.updateUser(userDto, id));
    }

    /** Admin only: delete user */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) throws ResourceNotFoundException {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    /** Any authenticated user: upload own profile photo */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadUserPhotoProfile(@RequestParam("file") MultipartFile file) throws IOException {
        userService.uploadUserProfile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body("User photo profile upload success!");
    }

    /** Any authenticated user: get own profile photo */
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/get/photo", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getCurrentUserPhotoProfile() throws IOException, ResourceNotFoundException {
        byte[] response = userService.getCurrentUserPhotoProfile();
        return ResponseEntity.ok(response);
    }

    /** Any authenticated user: delete own profile photo */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/photo")
    public ResponseEntity<String> deleteUserPhotoProfile() throws ResourceNotFoundException {
        userService.deleteUserProfile();
        return ResponseEntity.ok("User profile delete success!");
    }

    // ==================== Forgot Password ====================

    @PostMapping("/forgot")
    public ResponseEntity<String> processForgotPasswordRequest(
            @RequestParam("email") String email,
            HttpServletRequest request
    ) {
        userService.processForgotPasswordRequest(email, request);
        // Always same message — anti email enumeration
        return ResponseEntity.ok(
                "If an account with that email exists, a password reset link has been sent.");
    }

    @GetMapping("/reset")
    public ResponseEntity<String> forgetPasswordTokenValidation(@RequestParam("token") String token)
            throws IllegalArgumentException, TokenHasExpiredException {
        userService.resetTokenValidation(token);
        return ResponseEntity.ok("token valid");
    }

    @PutMapping("/resetPass")
    public ResponseEntity<String> updateNewPassword(
            @RequestBody String password,
            @RequestParam("token") String token
    ) {
        userService.updateNewPassword(password, token);
        return ResponseEntity.ok("Password successfully updated");
    }
}
