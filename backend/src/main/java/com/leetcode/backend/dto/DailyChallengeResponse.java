package com.leetcode.backend.dto;

import com.leetcode.backend.model.DailyChallenge;
import com.leetcode.backend.model.Problem;
import java.time.LocalDate;

public record DailyChallengeResponse(Long id, LocalDate challengeDate, ProblemSummary problem, boolean completed) {
    public static DailyChallengeResponse from(DailyChallenge challenge, boolean completed) {
        Problem problem = challenge.getProblem();
        return new DailyChallengeResponse(challenge.getId(), challenge.getChallengeDate(),
                problem == null ? null : ProblemSummary.from(problem), completed);
    }

    public record ProblemSummary(Long id, String title, String description, String difficulty, String tags) {
        static ProblemSummary from(Problem problem) {
            return new ProblemSummary(problem.getId(), problem.getTitle(), problem.getDescription(),
                    problem.getDifficulty(), problem.getTags());
        }
    }
}
