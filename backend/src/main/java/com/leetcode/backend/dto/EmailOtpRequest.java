package com.leetcode.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailOtpRequest(@NotBlank @Email @Size(max = 150) String email) { }
