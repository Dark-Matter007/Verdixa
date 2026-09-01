package com.leetcode.backend.controller;

import com.leetcode.backend.dto.AuthResponse;
import com.leetcode.backend.dto.LoginRequest;
import com.leetcode.backend.dto.RegisterRequest;
import com.leetcode.backend.model.User;
import com.leetcode.backend.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        AuthResponse response = new AuthResponse(
                "Registration successful",
                user.getUsername(),
                user.getRole().name(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request) {

        User user = authService.authenticate(request);

        String token = authService.generateToken(user);

        AuthResponse response = new AuthResponse(
                "Login successful",
                user.getUsername(),
                user.getRole().name(),
                token
        );

        return ResponseEntity.ok(response);
    }
}