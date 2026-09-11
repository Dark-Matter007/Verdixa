package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A single short-lived, BCrypt-protected password reset code per account. */
@Entity
@Table(name = "password_reset_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Column(name = "otp_hash", nullable = false, length = 100) private String otpHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "resend_available_at", nullable = false) private LocalDateTime resendAvailableAt;
    @Column(nullable = false) private int failedAttempts;
    @Column(name = "issue_window_started_at", nullable = false) private LocalDateTime issueWindowStartedAt;
    @Column(nullable = false) private int issueCount;
    @Column(name = "consumed_at") private LocalDateTime consumedAt;
    public Long getId(){ return id; } public User getUser(){ return user; } public void setUser(User value){user=value;}
    public String getOtpHash(){return otpHash;} public void setOtpHash(String value){otpHash=value;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
    public LocalDateTime getResendAvailableAt(){return resendAvailableAt;} public void setResendAvailableAt(LocalDateTime value){resendAvailableAt=value;}
    public int getFailedAttempts(){return failedAttempts;} public void setFailedAttempts(int value){failedAttempts=value;}
    public LocalDateTime getIssueWindowStartedAt(){return issueWindowStartedAt;} public void setIssueWindowStartedAt(LocalDateTime value){issueWindowStartedAt=value;}
    public int getIssueCount(){return issueCount;} public void setIssueCount(int value){issueCount=value;}
    public LocalDateTime getConsumedAt(){return consumedAt;} public void setConsumedAt(LocalDateTime value){consumedAt=value;}
}
