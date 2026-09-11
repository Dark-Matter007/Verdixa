package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Provider identity metadata only; provider access and refresh tokens are never persisted. */
@Entity
@Table(name = "user_oauth_accounts", uniqueConstraints = @UniqueConstraint(name = "uk_oauth_provider_subject", columnNames = {"provider", "provider_user_id"}), indexes = @Index(name = "idx_oauth_provider_email", columnList = "provider_email"))
public class UserOAuthAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private OAuthProvider provider;
    @Column(name = "provider_user_id", nullable = false, length = 160) private String providerUserId;
    @Column(name = "provider_email", nullable = false, length = 150) private String providerEmail;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User value){user=value;}
    public OAuthProvider getProvider(){return provider;} public void setProvider(OAuthProvider value){provider=value;}
    public String getProviderUserId(){return providerUserId;} public void setProviderUserId(String value){providerUserId=value;}
    public String getProviderEmail(){return providerEmail;} public void setProviderEmail(String value){providerEmail=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
