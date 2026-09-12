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
    private void disabled(String type) { log.warn("Email delivery is disabled; a {} email was not delivered.", type); }
}
