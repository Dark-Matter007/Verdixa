package com.leetcode.backend.service;

import java.time.LocalDateTime;

/** Deliberately small boundary between application workflows and SMTP delivery. */
public interface EmailService {
    void sendVerificationCode(String recipient, String username, String otp);
    void sendPasswordResetCode(String recipient, String username, String otp);
    void sendWelcome(String recipient, String username);
    void sendUsernameChangeCode(String recipient, String username, String requestedUsername, String otp);
    void sendUsernameChanged(String recipient, String previousUsername, String newUsername);
    void sendEmailChangeCurrentCode(String recipient, String username, String requestedEmail, String otp);
    void sendEmailChangeNewCode(String recipient, String username, String otp);
    void sendEmailChangedNotice(String recipient, String username, String newEmail);
    void sendPasswordSetupCode(String recipient, String username, String otp);
    void sendPasswordChangedNotice(String recipient, String username);
    void sendContestRegistration(String recipient, String username, String contestTitle,
                                 String description, LocalDateTime startAt, LocalDateTime endAt,
                                 int problemCount, Long contestId);
    void sendContestNotice(String recipient, String username, String title, LocalDateTime startAt, LocalDateTime endAt, Long contestId, String type);
    void sendCreatorOtp(String recipient, String username, String otp);
    void sendCreatorDecision(String recipient, String username, boolean approved, String reason);
    void sendAssessmentInvitation(String recipient, String username, String title, String host, String organization, LocalDateTime startAt, LocalDateTime endAt, Long assessmentId);
    void sendAssessmentNotice(String recipient, String username, String title, String host, String organization,
                              String description, LocalDateTime startAt, LocalDateTime endAt,
                              LocalDateTime registrationDeadline, int problemCount, boolean fullscreenRequired,
                              boolean microphoneRequired, Long assessmentId, String type, String accessToken);
    void sendAssessmentAccessOtp(String recipient, String name, String assessmentTitle, String otp);
    void sendAssessmentAccessInvitation(String recipient,String name,String title,String host,String organization,LocalDateTime startAt,LocalDateTime endAt,int problemCount,boolean fullscreen,boolean microphone,String accessUrl);
}
