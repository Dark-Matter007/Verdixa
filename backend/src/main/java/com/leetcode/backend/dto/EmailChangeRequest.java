package com.leetcode.backend.dto;

import jakarta.validation.constraints.*;

public record EmailChangeRequest(
        @NotBlank @Size(max = 152) String newEmail,
        @Size(max = 100) String currentPassword) { }
