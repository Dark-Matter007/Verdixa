package com.leetcode.backend.controller;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.User;
import com.leetcode.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private static final String OTP_SENT = "If an account requires verification, a code has been sent.";
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }
    @PostMapping("/register") public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) { authService.register(request); return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(OTP_SENT)); }
    @PostMapping("/verify-email-otp") public MessageResponse verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) { authService.verifyEmailOtp(request); return new MessageResponse("Account verified successfully. You can now sign in."); }
    @PostMapping("/resend-email-otp") public MessageResponse resendEmailOtp(@Valid @RequestBody EmailOtpRequest request) { authService.resendEmailOtp(request.email()); return new MessageResponse(OTP_SENT); }
    @PostMapping("/login") public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) { User user = authService.authenticate(request); return ResponseEntity.ok(new AuthResponse("Login successful", user.getUsername(), user.getRole().name(), authService.generateToken(user))); }
}
