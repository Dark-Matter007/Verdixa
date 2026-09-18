package com.leetcode.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Explicit browser contract for preflight and the secured assessment bootstrap. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssessmentPreflightResponse(
        @JsonProperty("id") Long assessmentId,
        String title,
        String organization,
        String hostDisplayName,
        String description,
        String instructions,
        LocalDateTime startAt,
        LocalDateTime endAt,
        long durationMinutes,
        boolean fullscreenRequired,
        boolean microphoneRequired,
        boolean screenShareRequired,
        boolean strictProctoring,
        boolean inviteOnly,
        String maskedEmail,
        int problemCount,
        int mcqCount,
        int questionCount,
        String registrationStatus,
        boolean participantEligible,
        boolean available,
        LocalDateTime serverTime,
        List<Map<String, Object>> problems,
        List<Map<String, Object>> mcqs
) {}
