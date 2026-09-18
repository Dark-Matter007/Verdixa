package com.leetcode.backend.dto;
import jakarta.validation.constraints.*;
public record AssessmentOtpRequest(@NotNull Long accessId, @NotBlank @Pattern(regexp="\\d{6}") String otp) {}
