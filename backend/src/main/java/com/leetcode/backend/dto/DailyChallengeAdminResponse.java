package com.leetcode.backend.dto;

import java.time.LocalDate;

public record DailyChallengeAdminResponse(Long id, LocalDate challengeDate,
        DailyChallengeResponse.ProblemSummary problem, long participants,
        long successfulCompletions, long completionRate) { }
