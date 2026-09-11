package com.leetcode.backend.dto;

import com.leetcode.backend.model.ContestProblem;

/** A problem as it appears to one contestant. Status is scoped to this contest only. */
public record ContestProblemResponse(
        Long id, Long problemId, String title, String difficulty, int displayOrder, int points, String status) {
    public static ContestProblemResponse from(ContestProblem problem) {
        return from(problem, "UNOPENED");
    }

    public static ContestProblemResponse from(ContestProblem problem, String status) {
        return new ContestProblemResponse(problem.getId(), problem.getProblem().getId(), problem.getProblem().getTitle(),
                problem.getProblem().getDifficulty(), problem.getDisplayOrder(), problem.getPoints(), status);
    }
}
