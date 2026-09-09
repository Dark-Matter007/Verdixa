package com.leetcode.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Admin-only contest detail. It intentionally contains only account data needed for administration. */
public record AdminContestDetailResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        long problemCount,
        long registrationCount,
        List<ContestProblem> problems,
        List<Registration> registrations) {

    public record ContestProblem(Long id, String title, String difficulty, List<String> tags, int order) { }

    public record Registration(
            Long userId,
            String username,
            String email,
            LocalDateTime registeredAt,
            long solvedCount,
            long totalProblems) { }
}
