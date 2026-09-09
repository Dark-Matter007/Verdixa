package com.leetcode.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Stores only a BCrypt digest of the short-lived email verification code. */
@Entity
@Table(name = "email_verification_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "resend_available_at", nullable = false)
    private LocalDateTime resendAvailableAt;
    @Column(nullable = false)
    private int failedAttempts;
    @Column(name = "issue_window_started_at", nullable = false)
    private LocalDateTime issueWindowStartedAt;
    @Column(nullable = false)
    private int issueCount;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getResendAvailableAt() { return resendAvailableAt; }
    public void setResendAvailableAt(LocalDateTime resendAvailableAt) { this.resendAvailableAt = resendAvailableAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getIssueWindowStartedAt() { return issueWindowStartedAt; }
    public void setIssueWindowStartedAt(LocalDateTime issueWindowStartedAt) { this.issueWindowStartedAt = issueWindowStartedAt; }
    public int getIssueCount() { return issueCount; }
    public void setIssueCount(int issueCount) { this.issueCount = issueCount; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}
