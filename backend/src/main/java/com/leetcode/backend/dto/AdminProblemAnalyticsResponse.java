package com.leetcode.backend.dto;
import java.util.Map;
/** Aggregate-only problem analytics; hidden testcase payloads are intentionally absent. */
public record AdminProblemAnalyticsResponse(Long problemId, String title, String executionMode, boolean published,
    long totalSubmissions, long acceptedSubmissions, long acceptanceRate, long uniqueAttemptedUsers,
    long uniqueSolvedUsers, long publicTestCases, long hiddenTestCases,
    Map<String, Long> verdictDistribution, Map<String, Long> languageDistribution) {}
