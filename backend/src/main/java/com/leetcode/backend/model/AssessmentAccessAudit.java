package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable, low-sensitivity audit trail for anonymous assessment access. */
@Entity
@Table(name = "assessment_access_audits", indexes = {
        @Index(name = "idx_assessment_access_audit_assessment", columnList = "assessment_id,created_at"),
        @Index(name = "idx_assessment_access_audit_access", columnList = "access_id,created_at")
})
public class AssessmentAccessAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assessment_id", nullable = false) private Assessment assessment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "access_id") private AssessmentAccess access;
    @Column(nullable = false, length = 40) private String event;
    @Column(name = "ip_hash", length = 64) private String ipHash;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void created(){createdAt=LocalDateTime.now();}
    public void setAssessment(Assessment value){assessment=value;} public void setAccess(AssessmentAccess value){access=value;}
    public void setEvent(String value){event=value;} public void setIpHash(String value){ipHash=value;}
}
