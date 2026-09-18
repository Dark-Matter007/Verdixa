package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A purpose-limited, email-verified credential for one assessment. */
@Entity
@Table(name = "assessment_accesses", uniqueConstraints = @UniqueConstraint(name = "uk_assessment_access_email", columnNames = {"assessment_id", "email"}), indexes = @Index(name = "idx_assessment_access_grant", columnList = "assessment_id,grant_expires_at"))
public class AssessmentAccess {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assessment_id", nullable = false) private Assessment assessment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invitation_id") private AssessmentInvitation invitation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "participant_id") private User participant;
    @Column(nullable = false, length = 150) private String email;
    @Column(name = "full_name", nullable = false, length = 160) private String fullName;
    @Column(name = "participant_reference", length = 100) private String participantReference;
    @Column(length = 180) private String organization;
    @Column(name = "otp_hash", length = 100) private String otpHash;
    @Column(name = "otp_expires_at") private LocalDateTime otpExpiresAt;
    @Column(name = "resend_available_at") private LocalDateTime resendAvailableAt;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "issue_window_started_at") private LocalDateTime issueWindowStartedAt;
    @Column(name = "issue_count", nullable = false) private int issueCount;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @Column(name = "grant_hash", length = 100) private String grantHash;
    @Column(name = "grant_expires_at") private LocalDateTime grantExpiresAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void created(){createdAt=updatedAt=LocalDateTime.now();}
    @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public Assessment getAssessment(){return assessment;} public void setAssessment(Assessment v){assessment=v;}
    public AssessmentInvitation getInvitation(){return invitation;} public void setInvitation(AssessmentInvitation v){invitation=v;}
    public User getParticipant(){return participant;} public void setParticipant(User v){participant=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getParticipantReference(){return participantReference;} public void setParticipantReference(String v){participantReference=v;} public String getOrganization(){return organization;} public void setOrganization(String v){organization=v;}
    public String getOtpHash(){return otpHash;} public void setOtpHash(String v){otpHash=v;} public LocalDateTime getOtpExpiresAt(){return otpExpiresAt;} public void setOtpExpiresAt(LocalDateTime v){otpExpiresAt=v;}
    public LocalDateTime getResendAvailableAt(){return resendAvailableAt;} public void setResendAvailableAt(LocalDateTime v){resendAvailableAt=v;} public int getFailedAttempts(){return failedAttempts;} public void setFailedAttempts(int v){failedAttempts=v;}
    public LocalDateTime getIssueWindowStartedAt(){return issueWindowStartedAt;} public void setIssueWindowStartedAt(LocalDateTime v){issueWindowStartedAt=v;} public int getIssueCount(){return issueCount;} public void setIssueCount(int v){issueCount=v;}
    public LocalDateTime getVerifiedAt(){return verifiedAt;} public void setVerifiedAt(LocalDateTime v){verifiedAt=v;} public String getGrantHash(){return grantHash;} public void setGrantHash(String v){grantHash=v;}
    public LocalDateTime getGrantExpiresAt(){return grantExpiresAt;} public void setGrantExpiresAt(LocalDateTime v){grantExpiresAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
