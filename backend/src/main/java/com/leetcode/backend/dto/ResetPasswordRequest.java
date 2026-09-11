package com.leetcode.backend.dto;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Email @Size(max=150) String email, @Pattern(regexp="\\d{6}") String otp,
                                   @NotBlank @Size(min=6,max=100) String password, @NotBlank @Size(min=6,max=100) String confirmPassword) {}
