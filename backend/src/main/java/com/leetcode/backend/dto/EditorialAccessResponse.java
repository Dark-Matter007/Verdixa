package com.leetcode.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EditorialAccessResponse(String status, long failedSubmissionCount, int requiredFailedSubmissions,
        long remainingFailedSubmissions, boolean accepted, boolean published, EditorialResponse editorial) {}
