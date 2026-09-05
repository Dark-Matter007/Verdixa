package com.leetcode.backend.dto;
import jakarta.validation.constraints.*;
public record HintRequest(@NotBlank @Size(max=180) String title, @NotBlank @Size(max=10000) String content, @Min(0) Integer penaltyPoints, Boolean active) {}
