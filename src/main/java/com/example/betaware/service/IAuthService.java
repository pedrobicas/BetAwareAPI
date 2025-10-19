package com.example.betaware.service;

import com.example.betaware.dto.JwtResponse;
import com.example.betaware.dto.LoginRequest;
import com.example.betaware.dto.RegisterRequest;

public interface IAuthService {
    JwtResponse login(LoginRequest loginRequest);
    void register(RegisterRequest registerRequest);
}