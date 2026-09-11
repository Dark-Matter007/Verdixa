package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** SHA-256 digest of a short-lived browser hand-off code, never a JWT. */
@Entity
@Table(name = "oauth_login_codes", indexes = @Index(name = "idx_oauth_login_code_hash", columnList = "code_hash", unique = true))
public class OAuthLoginCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "code_hash", nullable = false, length = 64, unique = true) private String codeHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "consumed_at") private LocalDateTime consumedAt;
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User value){user=value;}
    public String getCodeHash(){return codeHash;} public void setCodeHash(String value){codeHash=value;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
    public LocalDateTime getConsumedAt(){return consumedAt;} public void setConsumedAt(LocalDateTime value){consumedAt=value;}
}
