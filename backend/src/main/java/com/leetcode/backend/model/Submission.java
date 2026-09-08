package com.leetcode.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import com.leetcode.backend.dto.JudgeTestCaseResult;

@Entity
@Table(name = "submissions", indexes = @Index(name = "idx_submission_user_problem_status", columnList = "user_id,problem_id,status"))
public class Submission {

    @Transient private com.leetcode.backend.dto.CertificateProgressResponse certificateProgress;
    public com.leetcode.backend.dto.CertificateProgressResponse getCertificateProgress(){return certificateProgress;}
    public void setCertificateProgress(com.leetcode.backend.dto.CertificateProgressResponse value){certificateProgress=value;}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /** Null for ordinary practice submissions.  Contest submissions retain the
     * immutable contest context used for standings and audit history. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_problem_id")
    private ContestProblem contestProblem;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(nullable = false, length = 20)
    private String language;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "passed_test_cases", nullable = false)
    private int passedTestCases = 0;

    @Column(name = "total_test_cases", nullable = false)
    private int totalTestCases = 0;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Transient
    private List<JudgeTestCaseResult> testCaseResults;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    public Submission() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }
    public Contest getContest() { return contest; }
    public void setContest(Contest contest) { this.contest = contest; }
    public ContestProblem getContestProblem() { return contestProblem; }
    public void setContestProblem(ContestProblem contestProblem) { this.contestProblem = contestProblem; }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public int getPassedTestCases() {
        return passedTestCases;
    }

    public void setPassedTestCases(int passedTestCases) {
        this.passedTestCases = passedTestCases;
    }

    public int getTotalTestCases() {
        return totalTestCases;
    }

    public void setTotalTestCases(int totalTestCases) {
        this.totalTestCases = totalTestCases;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    public List<JudgeTestCaseResult> getTestCaseResults() { return testCaseResults; }
    public void setTestCaseResults(List<JudgeTestCaseResult> value) { testCaseResults = value; }
}
