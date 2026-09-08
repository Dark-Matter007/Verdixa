package com.leetcode.backend.dto;
import jakarta.validation.constraints.*;
public record EditorialRequest(@NotBlank @Size(max=180) String title, @NotBlank String intuition, @NotBlank String approach, @NotBlank String algorithmExplanation, String edgeCases, @NotBlank @Size(max=120) String timeComplexity, @NotBlank @Size(max=120) String spaceComplexity, @NotBlank String javaSolution, @NotBlank String cppSolution, @NotBlank String pythonSolution, boolean published, String keyObservations, String correctness, String referenceGuidance) {}
