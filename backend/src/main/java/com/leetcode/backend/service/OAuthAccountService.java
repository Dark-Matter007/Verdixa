package com.leetcode.backend.service;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.UserOAuthAccountRepository;
import com.leetcode.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class OAuthAccountService {
    private final UserRepository users;
    private final UserOAuthAccountRepository accounts;

    public OAuthAccountService(UserRepository users, UserOAuthAccountRepository accounts) { this.users = users; this.accounts = accounts; }

    /** Resolves provider subject first, then links only against a provider-verified normalized email. */
    @Transactional
    public User resolve(OAuthIdentity identity) {
        if (identity.providerUserId() == null || identity.providerUserId().isBlank()) throw new OAuthLoginException("oauth_failed");
        if (!identity.emailVerified() || identity.email() == null || identity.email().isBlank()) throw new OAuthLoginException("missing_verified_email");
        String email = normalizeEmail(identity.email());
        UserOAuthAccount linked = accounts.findByProviderAndProviderUserId(identity.provider(), identity.providerUserId()).orElse(null);
        if (linked != null) return linked.getUser();

        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> createUser(identity, email));
        UserOAuthAccount account = new UserOAuthAccount();
        account.setUser(user); account.setProvider(identity.provider()); account.setProviderUserId(identity.providerUserId());
        account.setProviderEmail(email); account.setCreatedAt(LocalDateTime.now()); account.setUpdatedAt(LocalDateTime.now());
        accounts.save(account);
        return user;
    }

    private User createUser(OAuthIdentity identity, String email) {
        User user = new User();
        user.setUsername(nextUsername(identity.displayName(), identity.providerLogin(), email));
        user.setEmail(email); user.setPassword(null); user.setRole(Role.USER); user.setEmailVerified(true);
        return users.save(user);
    }

    private String nextUsername(String displayName, String providerLogin, String email) {
        String source = firstUsable(providerLogin, displayName, email.substring(0, email.indexOf('@')));
        String base = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.length() < 3) base = "verdixa_user";
        if (base.length() > 88) base = base.substring(0, 88);
        String candidate = base;
        for (int suffix = 2; users.existsByUsernameIgnoreCase(candidate); suffix++) candidate = base.substring(0, Math.min(base.length(), 100 - String.valueOf(suffix).length())) + suffix;
        return candidate;
    }

    private String firstUsable(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return "verdixa_user"; }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
