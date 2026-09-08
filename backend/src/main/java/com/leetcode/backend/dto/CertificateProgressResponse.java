package com.leetcode.backend.dto;
import java.time.Instant;
import java.util.List;
public record CertificateProgressResponse(long solvedCount, int currentMilestone, Integer nextMilestone,
        List<MilestoneProgress> milestones, List<CertificateResponse> newlyIssued) {
    public record MilestoneProgress(int milestone, boolean earned, long current, int target, long remaining,
        String certificateId, Instant issuedAt, String verificationStatus) {}
}
