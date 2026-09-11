package com.leetcode.backend.dto;
import jakarta.validation.constraints.*;
public record AssistantChatRequest(@NotBlank @Size(max=1000) String message, @Size(max=40) String pageType, @Positive Long pageId) {}
