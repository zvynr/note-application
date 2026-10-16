package com.example.note.service;

import com.example.note.model.dto.JwtRequest;
import com.example.note.model.dto.JwtResponse;

public interface AuthService {
    JwtResponse login(JwtRequest authenticationRequest);
}
