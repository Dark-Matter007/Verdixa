package com.leetcode.backend.dto;
import jakarta.validation.constraints.*;
public record AssessmentAccessRequest(@NotBlank @Size(max=160) String fullName, @Email @Size(max=150) String email, @Size(max=100) String participantId, @Size(max=180) String organization, @NotBlank(message="Confirm the assessment safeguards before continuing.") String confirmation, @Size(max=300) String invite) {}
