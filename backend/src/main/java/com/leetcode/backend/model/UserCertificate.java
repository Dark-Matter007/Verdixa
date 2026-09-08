package com.leetcode.backend.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="user_certificates", uniqueConstraints={
    @UniqueConstraint(name="uk_certificate_user_milestone", columnNames={"user_id","milestone"}),
    @UniqueConstraint(name="uk_certificate_public_id", columnNames="public_certificate_id")})
public class UserCertificate {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable=false,updatable=false,length=25) private CertificateMilestone milestone;
    @Column(name="public_certificate_id",nullable=false,updatable=false,length=36) private String publicCertificateId;
    @Column(nullable=false,updatable=false) private Instant issuedAt;
    @Column(nullable=false,updatable=false) private long solvedCountAtIssue;
    @Column(nullable=false,updatable=false,length=36) private String verificationCode;
    @Column(nullable=false,updatable=false) private int certificateVersion;
    @Column(nullable=false,updatable=false,length=100) private String recipientName;
    protected UserCertificate() {}
    public UserCertificate(User user, CertificateMilestone milestone, long solvedCount) {
        this.user=user; this.milestone=milestone; solvedCountAtIssue=solvedCount;
        publicCertificateId=UUID.randomUUID().toString(); verificationCode=UUID.randomUUID().toString();
        issuedAt=Instant.now(); certificateVersion=1; recipientName=user.getUsername();
    }
    public String getPublicCertificateId(){return publicCertificateId;}
    public CertificateMilestone getMilestone(){return milestone;}
    public Instant getIssuedAt(){return issuedAt;}
    public long getSolvedCountAtIssue(){return solvedCountAtIssue;}
    public String getVerificationCode(){return verificationCode;}
    public int getCertificateVersion(){return certificateVersion;}
    public String getRecipientName(){return recipientName;}
}
