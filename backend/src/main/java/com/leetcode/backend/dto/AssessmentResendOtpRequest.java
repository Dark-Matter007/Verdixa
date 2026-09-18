package com.leetcode.backend.dto;
import jakarta.validation.constraints.NotNull;
public record AssessmentResendOtpRequest(@NotNull Long accessId) {}
