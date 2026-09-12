package com.leetcode.backend.dto;

import jakarta.validation.constraints.*;

public record OtpCodeRequest(@NotBlank @Pattern(regexp = "\\d{6}") String otp) { }
