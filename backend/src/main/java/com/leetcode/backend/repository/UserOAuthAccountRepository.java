package com.leetcode.backend.repository;

import com.leetcode.backend.model.OAuthProvider;
import com.leetcode.backend.model.UserOAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, Long> {
    Optional<UserOAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    List<UserOAuthAccount> findByUserId(Long userId);
}
