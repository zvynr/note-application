package com.example.note.controller;

import com.example.note.model.dto.JwtRequest;
import com.example.note.model.dto.JwtResponse;
import com.example.note.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> createAuthenticationToken(@Valid @RequestBody JwtRequest authenticationRequest) {
        JwtResponse response = authService.login(authenticationRequest);
        return ResponseEntity.ok(response);
    }
}
