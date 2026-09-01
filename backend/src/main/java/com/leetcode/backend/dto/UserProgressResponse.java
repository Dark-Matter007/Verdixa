package com.leetcode.backend.dto;

import com.leetcode.backend.model.Role;

import java.util.List;

public class UserProgressResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;
    private final int totalProblems;
    private final int solvedProblems;
    private final int easySolved;
    private final int mediumSolved;
    private final int hardSolved;
    private final int completionPercentage;
    private final int submissionCount;
    private final int acceptedSubmissionCount;
    private final int acceptanceRate;
    private final int currentStreak;
    private final List<Long> solvedProblemIds;

    public UserProgressResponse(
            Long id,
            String username,
            String email,
            Role role,
            int totalProblems,
            int solvedProblems,
            int easySolved,
            int mediumSolved,
            int hardSolved,
            int completionPercentage,
            int submissionCount,
            int acceptedSubmissionCount,
            int acceptanceRate,
            int currentStreak,
            List<Long> solvedProblemIds) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.totalProblems = totalProblems;
        this.solvedProblems = solvedProblems;
        this.easySolved = easySolved;
        this.mediumSolved = mediumSolved;
        this.hardSolved = hardSolved;
        this.completionPercentage = completionPercentage;
        this.submissionCount = submissionCount;
        this.acceptedSubmissionCount = acceptedSubmissionCount;
        this.acceptanceRate = acceptanceRate;
        this.currentStreak = currentStreak;
        this.solvedProblemIds = solvedProblemIds;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public int getTotalProblems() { return totalProblems; }
    public int getSolvedProblems() { return solvedProblems; }
    public int getEasySolved() { return easySolved; }
    public int getMediumSolved() { return mediumSolved; }
    public int getHardSolved() { return hardSolved; }
    public int getCompletionPercentage() { return completionPercentage; }
    public int getSubmissionCount() { return submissionCount; }
    public int getAcceptedSubmissionCount() { return acceptedSubmissionCount; }
    public int getAcceptanceRate() { return acceptanceRate; }
    public int getCurrentStreak() { return currentStreak; }
    public List<Long> getSolvedProblemIds() { return solvedProblemIds; }
}
