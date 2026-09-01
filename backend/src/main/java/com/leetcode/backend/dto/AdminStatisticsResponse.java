package com.leetcode.backend.dto;

/** Database-derived platform totals for the administrative dashboard. */
public record AdminStatisticsResponse(
        long totalUsers,
        long userCount,
        long adminCount,
        long totalProblems,
        long activeProblems,
        long inactiveProblems,
        long totalSubmissions,
        long acceptedSubmissions,
        long wrongAnswerSubmissions,
        long compilationErrors,
        long runtimeErrors,
        long timeLimitExceeded,
        int acceptanceRate) {
}
