package com.leetcode.backend.service;

import com.leetcode.backend.model.OAuthProvider;

/** Verified identity data supplied by Spring Security's provider-specific user service. */
public record OAuthIdentity(OAuthProvider provider, String providerUserId, String email,
                            boolean emailVerified, String displayName, String providerLogin) { }
