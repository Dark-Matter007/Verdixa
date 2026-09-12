package com.leetcode.backend.dto;

import jakarta.validation.constraints.*;

public record PasswordChangeRequest(
        @NotBlank @Size(max = 100) String currentPassword,
        @NotBlank @Size(min = 6, max = 100) String newPassword,
        @NotBlank @Size(min = 6, max = 100) String confirmPassword) { }
