package com.leetcode.backend.dto;

import jakarta.validation.constraints.*;

public record UsernameChangeRequest(
        @NotBlank @Size(min = 3, max = 100)
        @Pattern(regexp = "[A-Za-z0-9_][A-Za-z0-9_.-]*", message = "Username may contain letters, numbers, dots, underscores, and hyphens.")
        String username) { }
