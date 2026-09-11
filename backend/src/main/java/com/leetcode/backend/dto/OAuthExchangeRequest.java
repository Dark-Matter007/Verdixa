package com.leetcode.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record OAuthExchangeRequest(@NotBlank @Size(max = 200) String code) { }
