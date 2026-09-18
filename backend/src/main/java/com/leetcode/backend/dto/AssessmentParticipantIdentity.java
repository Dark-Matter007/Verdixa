package com.leetcode.backend.dto;

/** Scalar identity returned across the assessment authorization transaction boundary. */
public record AssessmentParticipantIdentity(Long userId, String username) {}
