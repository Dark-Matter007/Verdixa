package com.leetcode.backend.dto;
import jakarta.validation.constraints.*;
public record VerifyPasswordResetOtpRequest(@NotBlank @Email @Size(max=150) String email, @Pattern(regexp="\\d{6}") String otp) {}
