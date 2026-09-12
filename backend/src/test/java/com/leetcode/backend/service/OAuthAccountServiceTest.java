package com.leetcode.backend.service;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.UserOAuthAccountRepository;
import com.leetcode.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {
    @Mock UserRepository users;
    @Mock UserOAuthAccountRepository accounts;
    @InjectMocks OAuthAccountService service;

    @Test void verifiedGoogleIdentityCreatesVerifiedUserWithoutPassword() {
        when(accounts.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-subject")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.empty());
        when(users.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.resolve(new OAuthIdentity(OAuthProvider.GOOGLE, "google-subject", "Ada@Example.com", true, "Ada Lovelace", null));

        assertEquals("ada_lovelace", result.getUsername());
        assertEquals("ada@example.com", result.getEmail());
        assertTrue(result.isEmailVerified()); assertEquals(Role.USER, result.getRole()); assertNull(result.getPassword());
        verify(accounts).save(argThat(link -> link.getUser() == result && link.getProvider() == OAuthProvider.GOOGLE));
    }

    @Test void matchingExistingAdminIsLinkedWithoutChangingRole() {
        User admin = new User("admin", "admin@example.com", "hash", Role.ADMIN); admin.setEmailVerified(true);
        when(accounts.findByProviderAndProviderUserId(OAuthProvider.GITHUB, "99")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        User result = service.resolve(new OAuthIdentity(OAuthProvider.GITHUB, "99", "admin@example.com", true, "Admin", "admin"));

        assertSame(admin, result); assertEquals(Role.ADMIN, result.getRole()); verify(users, never()).save(any());
        verify(accounts).save(argThat(link -> link.getUser() == admin && link.getProvider() == OAuthProvider.GITHUB));
    }

    @Test void unverifiedEmailIsRejectedBeforeAnyAccountWrite() {
        OAuthLoginException failure = assertThrows(OAuthLoginException.class, () -> service.resolve(new OAuthIdentity(OAuthProvider.GOOGLE, "subject", "user@example.com", false, "User", null)));
        assertEquals("missing_verified_email", failure.getReason()); verifyNoInteractions(users, accounts);
    }
}
