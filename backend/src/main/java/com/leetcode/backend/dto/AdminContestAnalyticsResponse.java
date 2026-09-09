package com.leetcode.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Admin-only contest participation summary. Deliberately excludes email and other account data. */
public record AdminContestAnalyticsResponse(
        long registrationCount,
        long participantsWhoSolved,
        long contestProblemCount,
        long solvedProblemCount,
        List<RegisteredContestUser> registeredUsers) {

    public record RegisteredContestUser(String username, long solvedProblemCount, LocalDateTime registeredAt) { }
}
