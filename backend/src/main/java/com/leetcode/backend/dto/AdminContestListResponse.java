package com.leetcode.backend.dto;

import java.time.LocalDateTime;

/** Compact, schedule-derived row used by the admin contest registry. */
public record AdminContestListResponse(
        Long id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        long problemCount,
        long registrationCount) { }
