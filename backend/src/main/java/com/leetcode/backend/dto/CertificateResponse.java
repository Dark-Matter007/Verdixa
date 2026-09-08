package com.leetcode.backend.dto;
import com.leetcode.backend.model.UserCertificate;
import java.time.Instant;
public record CertificateResponse(String publicCertificateId, String recipientName, int milestone,
        Instant issuedAt, long solvedCountAtIssue, String verificationCode, int certificateVersion, String status) {
    public static CertificateResponse from(UserCertificate c) {
        return new CertificateResponse(c.getPublicCertificateId(),c.getRecipientName(),c.getMilestone().getTarget(),
            c.getIssuedAt(),c.getSolvedCountAtIssue(),c.getVerificationCode(),c.getCertificateVersion(),"VERIFIED");
    }
}
