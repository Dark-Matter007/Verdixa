package com.leetcode.backend.dto;

import com.leetcode.backend.model.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record ThemePreferenceRequest(@NotNull ThemePreference theme) {}
