package com.leetcode.backend.service;

import java.time.LocalDateTime;

public final class EmailNotificationEvents {
    private EmailNotificationEvents() { }
    public record OtpIssued(String email, String username, String otp) { }
    public record AccountVerified(String email, String username) { }
    public record ContestRegistered(String email, String username, String title, String description,
                                  LocalDateTime startAt, LocalDateTime endAt, int problemCount, Long contestId) { }
}
