package com.leetcode.backend.controller;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.User;
import com.leetcode.backend.service.AuthService;
import com.leetcode.backend.service.OAuthLoginCodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private static final String OTP_SENT = "If an account requires verification, a code has been sent.";
    private final AuthService authService;
    private final OAuthLoginCodeService oauthLoginCodes;
    public AuthController(AuthService authService, OAuthLoginCodeService oauthLoginCodes) { this.authService = authService; this.oauthLoginCodes = oauthLoginCodes; }
    @PostMapping("/register") public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) { authService.register(request); return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(OTP_SENT)); }
    @PostMapping("/verify-email-otp") public MessageResponse verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) { authService.verifyEmailOtp(request); return new MessageResponse("Account verified successfully. You can now sign in."); }
    @PostMapping("/resend-email-otp") public MessageResponse resendEmailOtp(@Valid @RequestBody EmailOtpRequest request) { authService.resendEmailOtp(request.email()); return new MessageResponse(OTP_SENT); }
    @PostMapping("/login") public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) { User user = authService.authenticate(request); return ResponseEntity.ok(new AuthResponse("Login successful", user.getUsername(), user.getRole().name(), authService.generateToken(user))); }
    @PostMapping("/oauth/exchange") public ResponseEntity<AuthResponse> exchangeOAuthCode(@Valid @RequestBody OAuthExchangeRequest request) { return ResponseEntity.ok(oauthLoginCodes.exchange(request.code())); }
    @PostMapping("/forgot-password") public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) { authService.requestPasswordReset(request.email()); return new MessageResponse("If an account exists for that email, a password reset code has been sent."); }
    @PostMapping("/resend-password-reset-otp") public MessageResponse resendPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) { authService.resendPasswordReset(request.email()); return new MessageResponse("If an account exists for that email, a password reset code has been sent."); }
    @PostMapping("/verify-password-reset-otp") public MessageResponse verifyPasswordReset(@Valid @RequestBody VerifyPasswordResetOtpRequest request) { authService.verifyPasswordResetOtp(request.email(), request.otp()); return new MessageResponse("Code verified. Choose a new password."); }
    @PostMapping("/reset-password") public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) { authService.resetPassword(request.email(), request.otp(), request.password(), request.confirmPassword()); return new MessageResponse("Password reset successfully. You can now sign in."); }
}
