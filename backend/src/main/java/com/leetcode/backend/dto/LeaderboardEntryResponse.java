package com.leetcode.backend.dto;

public class LeaderboardEntryResponse {

    private final int rank;
    private final Long userId;
    private final String username;
    private final int solvedProblems;
    private final int acceptedSubmissionCount;
    private final int submissionCount;
    private final int acceptanceRate;

    public LeaderboardEntryResponse(
            int rank,
            Long userId,
            String username,
            int solvedProblems,
            int acceptedSubmissionCount,
            int submissionCount,
            int acceptanceRate) {

        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.solvedProblems = solvedProblems;
        this.acceptedSubmissionCount = acceptedSubmissionCount;
        this.submissionCount = submissionCount;
        this.acceptanceRate = acceptanceRate;
    }

    public int getRank() { return rank; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getSolvedProblems() { return solvedProblems; }
    public int getAcceptedSubmissionCount() { return acceptedSubmissionCount; }
    public int getSubmissionCount() { return submissionCount; }
    public int getAcceptanceRate() { return acceptanceRate; }
}
