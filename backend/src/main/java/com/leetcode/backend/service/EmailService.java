package com.leetcode.backend.service;

import java.time.LocalDateTime;

/** Deliberately small boundary between application workflows and SMTP delivery. */
public interface EmailService {
    void sendVerificationCode(String recipient, String username, String otp);
    void sendWelcome(String recipient, String username);
    void sendContestRegistration(String recipient, String username, String contestTitle,
                                 String description, LocalDateTime startAt, LocalDateTime endAt,
                                 int problemCount, Long contestId);
}
