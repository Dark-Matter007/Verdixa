package com.leetcode.backend.service;

import java.time.LocalDateTime;

public final class EmailNotificationEvents {
    private EmailNotificationEvents() { }
    public record OtpIssued(String email, String username, String otp) { }
    public record PasswordResetOtpIssued(String email, String username, String otp) { }
    public record AccountVerified(String email, String username) { }
    public record UsernameChangeOtpIssued(String email, String username, String requestedUsername, String otp) { }
    public record UsernameChanged(String email, String previousUsername, String newUsername) { }
    public record EmailChangeCurrentOtpIssued(String email, String username, String requestedEmail, String otp) { }
    public record EmailChangeNewOtpIssued(String email, String username, String otp) { }
    public record EmailChanged(String previousEmail, String username, String newEmail) { }
    public record PasswordSetupOtpIssued(String email, String username, String otp) { }
    public record PasswordChanged(String email, String username) { }
    public record ContestRegistered(String email, String username, String title, String description,
                                  LocalDateTime startAt, LocalDateTime endAt, int problemCount, Long contestId) { }
    public record ContestCreated(Long contestId) { }
    public record CreatorOtpIssued(String email,String username,String otp) { }
    public record CreatorDecision(String email,String username,boolean approved,String reason) { }
    public record AssessmentPublished(Long assessmentId) { }
    public record AssessmentInvitationsChanged(Long assessmentId) { }
    public record AssessmentRegistered(Long assessmentId, Long userId) { }
    public record AssessmentUpdated(Long assessmentId) { }
    public record AssessmentCancelled(Long assessmentId) { }
    public record AssessmentAccessInvitation(String email,String name,String title,String host,String organization,LocalDateTime startAt,LocalDateTime endAt,int problemCount,boolean fullscreen,boolean microphone,String accessUrl) { }
}
