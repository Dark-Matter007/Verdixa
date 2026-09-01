package com.leetcode.backend.dto;

import com.leetcode.backend.model.Submission;

import java.time.LocalDateTime;
import java.util.List;

public class SubmissionResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long problemId;
    private String problemTitle;
    private String sourceCode;
    private String language;
    private String status;
    private String output;
    private String errorMessage;
    private Long executionTimeMs;
    private int passedTestCases;
    private int totalTestCases;
    private LocalDateTime submittedAt;
    private List<JudgeTestCaseResult> testCaseResults;

    public SubmissionResponse() {
    }

    public SubmissionResponse(Submission submission) {

        this.id = submission.getId();

        if (submission.getUser() != null) {
            this.userId = submission.getUser().getId();
            this.username = submission.getUser().getUsername();
        }

        if (submission.getProblem() != null) {
            this.problemId = submission.getProblem().getId();
            this.problemTitle = submission.getProblem().getTitle();
        }

        this.sourceCode = submission.getSourceCode();
        this.language = submission.getLanguage();
        this.status = submission.getStatus();
        this.output = submission.getOutput();
        this.errorMessage = submission.getErrorMessage();
        this.executionTimeMs = submission.getExecutionTimeMs();
        this.passedTestCases = submission.getPassedTestCases();
        this.totalTestCases = submission.getTotalTestCases();
        this.submittedAt = submission.getSubmittedAt();
        this.testCaseResults = submission.getTestCaseResults();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getProblemId() {
        return problemId;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getLanguage() {
        return language;
    }

    public String getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public int getPassedTestCases() {
        return passedTestCases;
    }

    public int getTotalTestCases() {
        return totalTestCases;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    public List<JudgeTestCaseResult> getTestCaseResults() { return testCaseResults; }
}
