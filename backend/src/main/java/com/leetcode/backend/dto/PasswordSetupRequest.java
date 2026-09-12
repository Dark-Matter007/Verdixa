package com.leetcode.backend.dto;

import jakarta.validation.constraints.*;

public record PasswordSetupRequest(
        @NotBlank @Size(min = 6, max = 100) String newPassword,
        @NotBlank @Size(min = 6, max = 100) String confirmPassword) { }
