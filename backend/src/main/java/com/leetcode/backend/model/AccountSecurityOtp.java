package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** BCrypt-protected, purpose-scoped OTP state for authenticated profile changes. */
@Entity
@Table(name = "account_security_otps",
        uniqueConstraints = @UniqueConstraint(name = "uk_account_security_otp_user_purpose", columnNames = {"user_id", "purpose"}))
public class AccountSecurityOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AccountOtpPurpose purpose;
    @Column(name = "otp_hash", nullable = false, length = 100) private String otpHash;
    @Column(name = "target_value", length = 150) private String targetValue;
    @Column(name = "issued_at", nullable = false) private LocalDateTime issuedAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "resend_available_at", nullable = false) private LocalDateTime resendAvailableAt;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "issue_window_started_at", nullable = false) private LocalDateTime issueWindowStartedAt;
    @Column(name = "issue_count", nullable = false) private int issueCount;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @Column(name = "consumed_at") private LocalDateTime consumedAt;

    public Long getId() { return id; }
    public User getUser() { return user; } public void setUser(User value) { user = value; }
    public AccountOtpPurpose getPurpose() { return purpose; } public void setPurpose(AccountOtpPurpose value) { purpose = value; }
    public String getOtpHash() { return otpHash; } public void setOtpHash(String value) { otpHash = value; }
    public String getTargetValue() { return targetValue; } public void setTargetValue(String value) { targetValue = value; }
    public LocalDateTime getIssuedAt() { return issuedAt; } public void setIssuedAt(LocalDateTime value) { issuedAt = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; } public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public LocalDateTime getResendAvailableAt() { return resendAvailableAt; } public void setResendAvailableAt(LocalDateTime value) { resendAvailableAt = value; }
    public int getFailedAttempts() { return failedAttempts; } public void setFailedAttempts(int value) { failedAttempts = value; }
    public LocalDateTime getIssueWindowStartedAt() { return issueWindowStartedAt; } public void setIssueWindowStartedAt(LocalDateTime value) { issueWindowStartedAt = value; }
    public int getIssueCount() { return issueCount; } public void setIssueCount(int value) { issueCount = value; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; } public void setVerifiedAt(LocalDateTime value) { verifiedAt = value; }
    public LocalDateTime getConsumedAt() { return consumedAt; } public void setConsumedAt(LocalDateTime value) { consumedAt = value; }
}
