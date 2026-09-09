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
    @Override public void sendWelcome(String recipient, String username) { log.warn("Email delivery is disabled; a welcome email was not delivered."); }
    @Override public void sendContestRegistration(String recipient, String username, String contestTitle, String description, LocalDateTime startAt, LocalDateTime endAt, int problemCount, Long contestId) { log.warn("Email delivery is disabled; a contest registration email was not delivered."); }
}
