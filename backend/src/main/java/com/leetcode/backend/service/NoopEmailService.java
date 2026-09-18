package com.leetcode.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Safe local default: no SMTP traffic is attempted until it is configured explicitly. */
@Service
public class NoopEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(NoopEmailService.class);
    @Override public void sendVerificationCode(String recipient, String username, String otp) { log.warn("Email delivery is disabled; a verification email was not delivered."); }
    @Override public void sendPasswordResetCode(String recipient, String username, String otp) { log.warn("Email delivery is disabled; a password reset email was not delivered."); }
    @Override public void sendWelcome(String recipient, String username) { log.warn("Email delivery is disabled; a welcome email was not delivered."); }
    @Override public void sendUsernameChangeCode(String recipient, String username, String requestedUsername, String otp) { disabled("username change code"); }
    @Override public void sendUsernameChanged(String recipient, String previousUsername, String newUsername) { disabled("username changed notice"); }
    @Override public void sendEmailChangeCurrentCode(String recipient, String username, String requestedEmail, String otp) { disabled("current email confirmation"); }
    @Override public void sendEmailChangeNewCode(String recipient, String username, String otp) { disabled("new email verification"); }
    @Override public void sendEmailChangedNotice(String recipient, String username, String newEmail) { disabled("email changed notice"); }
    @Override public void sendPasswordSetupCode(String recipient, String username, String otp) { disabled("password setup code"); }
    @Override public void sendPasswordChangedNotice(String recipient, String username) { disabled("password changed notice"); }
    @Override public void sendContestRegistration(String recipient, String username, String contestTitle, String description, LocalDateTime startAt, LocalDateTime endAt, int problemCount, Long contestId) { log.warn("Email delivery is disabled; a contest registration email was not delivered."); }
    @Override public void sendContestNotice(String recipient,String username,String title,LocalDateTime startAt,LocalDateTime endAt,Long id,String type){disabled(type);}
    @Override public void sendCreatorOtp(String recipient,String username,String otp){disabled("creator verification OTP");}
    @Override public void sendCreatorDecision(String recipient,String username,boolean approved,String reason){disabled("creator decision");}
    @Override public void sendAssessmentInvitation(String recipient,String username,String title,String host,String organization,LocalDateTime startAt,LocalDateTime endAt,Long id){disabled("assessment invitation");}
    @Override public void sendAssessmentNotice(String recipient,String username,String title,String host,String organization,String description,LocalDateTime startAt,LocalDateTime endAt,LocalDateTime registrationDeadline,int problemCount,boolean fullscreenRequired,boolean microphoneRequired,Long id,String type,String accessToken){disabled(type);}
    @Override public void sendAssessmentAccessOtp(String recipient,String name,String assessmentTitle,String otp){disabled("assessment verification OTP");}
    @Override public void sendAssessmentAccessInvitation(String recipient,String name,String title,String host,String organization,LocalDateTime startAt,LocalDateTime endAt,int problemCount,boolean fullscreen,boolean microphone,String accessUrl){disabled("assessment invitation");}
    private void disabled(String type) { log.warn("Email delivery is disabled; a {} email was not delivered.", type); }
}
