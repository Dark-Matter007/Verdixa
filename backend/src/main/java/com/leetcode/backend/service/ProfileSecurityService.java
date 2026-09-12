package com.leetcode.backend.service;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

import static com.leetcode.backend.model.AccountOtpPurpose.*;

@Service
public class ProfileSecurityService {
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_MAX_ISSUES_PER_HOUR = 5;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int OTP_EXPIRY_MINUTES = 10;

    private final UserRepository users;
    private final UserOAuthAccountRepository oauthAccounts;
    private final AccountSecurityOtpRepository otps;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    public ProfileSecurityService(UserRepository users, UserOAuthAccountRepository oauthAccounts,
                                  AccountSecurityOtpRepository otps, PasswordEncoder passwordEncoder,
                                  ApplicationEventPublisher events) {
        this.users = users;
        this.oauthAccounts = oauthAccounts;
        this.otps = otps;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String principalName) {
        User user = current(principalName);
        List<OAuthProvider> providers = oauthAccounts.findByUserId(user.getId()).stream()
                .map(UserOAuthAccount::getProvider).distinct().toList();
        return new ProfileResponse(user.getUsername(), user.getEmail(), user.getRole(), user.getTheme(),
                user.isEmailVerified(), user.getPassword() != null, providers);
    }

    @Transactional
    public ProfileActionResponse requestUsernameChange(String principalName, UsernameChangeRequest request) {
        User user = lockedCurrent(principalName);
        String requested = normalizeUsername(request.username());
        validateUsernameAvailable(user, requested);
        String code = issue(user, USERNAME_CHANGE, requested);
        events.publishEvent(new EmailNotificationEvents.UsernameChangeOtpIssued(user.getEmail(), user.getUsername(), requested, code));
        return ProfileActionResponse.pending("Enter the 6-digit code sent to your registered email.", "VERIFY_USERNAME", OTP_RESEND_COOLDOWN_SECONDS);
    }

    @Transactional(noRollbackFor = ProfileVerificationException.class)
    public ProfileActionResponse verifyUsernameChange(String principalName, OtpCodeRequest request) {
        User user = lockedCurrent(principalName);
        AccountSecurityOtp otp = pending(user, USERNAME_CHANGE);
        String requested = otp.getTargetValue();
        verify(otp, request.otp(), true);
        validateUsernameAvailable(user, requested);
        String previous = user.getUsername();
        user.setUsername(requested);
        user.incrementAuthVersion();
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException exception) { throw new IllegalArgumentException("That username is already in use."); }
        events.publishEvent(new EmailNotificationEvents.UsernameChanged(user.getEmail(), previous, requested));
        return ProfileActionResponse.changed("Username updated successfully. Please sign in again.");
    }

    @Transactional
    public ProfileActionResponse requestEmailChange(String principalName, EmailChangeRequest request) {
        User user = lockedCurrent(principalName);
        String requested = normalizeEmail(request.newEmail());
        validateEmailFormat(requested);
        validateEmailAvailable(user, requested);
        if (user.getPassword() != null && (request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPassword()))) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        String code = issue(user, EMAIL_CHANGE_CURRENT, requested);
        events.publishEvent(new EmailNotificationEvents.EmailChangeCurrentOtpIssued(user.getEmail(), user.getUsername(), requested, code));
        return ProfileActionResponse.pending("Confirm this change with the code sent to your current email.", "VERIFY_CURRENT_EMAIL", OTP_RESEND_COOLDOWN_SECONDS);
    }

    @Transactional(noRollbackFor = ProfileVerificationException.class)
    public ProfileActionResponse verifyCurrentEmail(String principalName, OtpCodeRequest request) {
        User user = lockedCurrent(principalName);
        AccountSecurityOtp currentOtp = pending(user, EMAIL_CHANGE_CURRENT);
        verify(currentOtp, request.otp(), true);
        String requested = currentOtp.getTargetValue();
        validateEmailAvailable(user, requested);
        String code = issue(user, EMAIL_CHANGE_NEW, requested);
        events.publishEvent(new EmailNotificationEvents.EmailChangeNewOtpIssued(requested, user.getUsername(), code));
        return ProfileActionResponse.pending("Current account confirmed. Enter the code sent to your new email.", "VERIFY_NEW_EMAIL", OTP_RESEND_COOLDOWN_SECONDS);
    }

    @Transactional
    public ProfileActionResponse resendNewEmailCode(String principalName) {
        User user = lockedCurrent(principalName);
        AccountSecurityOtp currentOtp = pending(user, EMAIL_CHANGE_CURRENT);
        requireConsumedAuthorization(currentOtp);
        String requested = currentOtp.getTargetValue();
        validateEmailAvailable(user, requested);
        String code = issue(user, EMAIL_CHANGE_NEW, requested);
        events.publishEvent(new EmailNotificationEvents.EmailChangeNewOtpIssued(requested, user.getUsername(), code));
        return ProfileActionResponse.pending("A new code was sent to your new email.", "VERIFY_NEW_EMAIL", OTP_RESEND_COOLDOWN_SECONDS);
    }

    @Transactional(noRollbackFor = ProfileVerificationException.class)
    public ProfileActionResponse verifyNewEmail(String principalName, OtpCodeRequest request) {
        User user = lockedCurrent(principalName);
        AccountSecurityOtp currentOtp = pending(user, EMAIL_CHANGE_CURRENT);
        requireConsumedAuthorization(currentOtp);
        AccountSecurityOtp newOtp = pending(user, EMAIL_CHANGE_NEW);
        if (!Objects.equals(currentOtp.getTargetValue(), newOtp.getTargetValue())) throw invalidOrExpired();
        verify(newOtp, request.otp(), true);
        String requested = newOtp.getTargetValue();
        validateEmailAvailable(user, requested);
        String previous = user.getEmail();
        user.setEmail(requested);
        user.setEmailVerified(true);
        user.incrementAuthVersion();
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException exception) { throw new IllegalArgumentException("This email address is already associated with another account."); }
        events.publishEvent(new EmailNotificationEvents.EmailChanged(previous, user.getUsername(), requested));
        return ProfileActionResponse.changed("Email address updated and verified successfully. Please sign in again.");
    }

    @Transactional
    public ProfileActionResponse changePassword(String principalName, PasswordChangeRequest request) {
        User user = lockedCurrent(principalName);
        if (user.getPassword() == null) throw new IllegalArgumentException("This account does not have a Verdixa password yet.");
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) throw new IllegalArgumentException("Current password is incorrect.");
        validateMatchingPasswords(request.newPassword(), request.confirmPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) throw new IllegalArgumentException("Choose a password you have not just been using.");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.incrementAuthVersion();
        users.save(user);
        events.publishEvent(new EmailNotificationEvents.PasswordChanged(user.getEmail(), user.getUsername()));
        return ProfileActionResponse.changed("Password updated successfully. Please sign in again.");
    }

    @Transactional
    public ProfileActionResponse requestPasswordSetup(String principalName) {
        User user = lockedCurrent(principalName);
        if (user.getPassword() != null) throw new IllegalArgumentException("This account already has a Verdixa password.");
        String code = issue(user, PASSWORD_SETUP, null);
        events.publishEvent(new EmailNotificationEvents.PasswordSetupOtpIssued(user.getEmail(), user.getUsername(), code));
        return ProfileActionResponse.pending("Enter the 6-digit code sent to your verified email.", "VERIFY_PASSWORD_SETUP", OTP_RESEND_COOLDOWN_SECONDS);
    }

    @Transactional(noRollbackFor = ProfileVerificationException.class)
    public ProfileActionResponse verifyPasswordSetup(String principalName, OtpCodeRequest request) {
        User user = lockedCurrent(principalName);
        if (user.getPassword() != null) throw new IllegalArgumentException("This account already has a Verdixa password.");
        verify(pending(user, PASSWORD_SETUP), request.otp(), false);
        return ProfileActionResponse.pending("Email confirmed. Choose your Verdixa password.", "SET_PASSWORD", 0);
    }

    @Transactional
    public ProfileActionResponse setupPassword(String principalName, PasswordSetupRequest request) {
        User user = lockedCurrent(principalName);
        if (user.getPassword() != null) throw new IllegalArgumentException("This account already has a Verdixa password.");
        AccountSecurityOtp otp = pending(user, PASSWORD_SETUP);
        requireVerified(otp);
        validateMatchingPasswords(request.newPassword(), request.confirmPassword());
        otp.setConsumedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.incrementAuthVersion();
        otps.save(otp);
        users.save(user);
        events.publishEvent(new EmailNotificationEvents.PasswordChanged(user.getEmail(), user.getUsername()));
        return ProfileActionResponse.changed("Verdixa password created successfully. Please sign in again.");
    }

    private User current(String name) {
        return users.findByUsernameIgnoreCase(name).orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private User lockedCurrent(String name) {
        User value = current(name);
        return users.lockById(value.getId()).orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private String issue(User user, AccountOtpPurpose purpose, String targetValue) {
        LocalDateTime now = LocalDateTime.now();
        AccountSecurityOtp otp = otps.findForUpdate(user.getId(), purpose).orElse(null);
        if (otp != null && otp.getResendAvailableAt().isAfter(now)) {
            long seconds = Math.max(1, java.time.Duration.between(now, otp.getResendAvailableAt()).toSeconds());
            throw new ProfileVerificationException("Please wait " + seconds + " seconds before requesting another code.");
        }
        if (otp == null) {
            otp = new AccountSecurityOtp();
            otp.setUser(user); otp.setPurpose(purpose); otp.setIssueWindowStartedAt(now); otp.setIssueCount(0);
        } else if (!otp.getIssueWindowStartedAt().plusHours(1).isAfter(now)) {
            otp.setIssueWindowStartedAt(now); otp.setIssueCount(0);
        }
        if (otp.getIssueCount() >= OTP_MAX_ISSUES_PER_HOUR) {
            throw new ProfileVerificationException("Too many verification codes requested. Try again later.");
        }
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        otp.setOtpHash(passwordEncoder.encode(code));
        otp.setTargetValue(targetValue);
        otp.setIssuedAt(now); otp.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setResendAvailableAt(now.plusSeconds(OTP_RESEND_COOLDOWN_SECONDS));
        otp.setFailedAttempts(0); otp.setVerifiedAt(null); otp.setConsumedAt(null);
        otp.setIssueCount(otp.getIssueCount() + 1);
        otps.save(otp);
        return code;
    }

    private AccountSecurityOtp pending(User user, AccountOtpPurpose purpose) {
        return otps.findForUpdate(user.getId(), purpose).orElseThrow(this::invalidOrExpired);
    }

    private void verify(AccountSecurityOtp otp, String code, boolean consume) {
        LocalDateTime now = LocalDateTime.now();
        if (otp.getConsumedAt() != null || !now.isBefore(otp.getExpiresAt())) throw invalidOrExpired();
        if (otp.getFailedAttempts() >= OTP_MAX_ATTEMPTS) throw new ProfileVerificationException("Too many incorrect attempts. Request a new code.");
        if (!passwordEncoder.matches(code, otp.getOtpHash())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            otps.save(otp);
            throw new ProfileVerificationException("The verification code is incorrect.");
        }
        otp.setVerifiedAt(now);
        if (consume) otp.setConsumedAt(now);
        otps.save(otp);
    }

    private void requireVerified(AccountSecurityOtp otp) {
        LocalDateTime now = LocalDateTime.now();
        if (otp.getVerifiedAt() == null || otp.getConsumedAt() != null || !now.isBefore(otp.getExpiresAt())) throw invalidOrExpired();
    }

    /** A consumed current-email OTP is the one-time authorization for the rest of that email-change flow. */
    private void requireConsumedAuthorization(AccountSecurityOtp otp) {
        LocalDateTime now = LocalDateTime.now();
        if (otp.getVerifiedAt() == null || otp.getConsumedAt() == null || !now.isBefore(otp.getExpiresAt())) throw invalidOrExpired();
    }

    private void validateUsernameAvailable(User user, String username) {
        if (user.getUsername().equalsIgnoreCase(username)) throw new IllegalArgumentException("Enter a username different from your current username.");
        users.findByUsernameIgnoreCase(username).filter(other -> !other.getId().equals(user.getId()))
                .ifPresent(other -> { throw new IllegalArgumentException("That username is already in use."); });
    }

    private void validateEmailAvailable(User user, String email) {
        if (user.getEmail().equalsIgnoreCase(email)) throw new IllegalArgumentException("Enter an email different from your current email.");
        users.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(user.getId()))
                .ifPresent(other -> { throw new IllegalArgumentException("This email address is already associated with another account."); });
    }

    private void validateEmailFormat(String email) {
        if (email.length() > 150) throw new IllegalArgumentException("Enter a valid email address.");
        try {
            jakarta.mail.internet.InternetAddress address = new jakarta.mail.internet.InternetAddress(email, true);
            address.validate();
            if (!email.contains("@")) throw new IllegalArgumentException();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
    }

    private void validateMatchingPasswords(String password, String confirmation) {
        if (!password.equals(confirmation)) throw new IllegalArgumentException("Passwords do not match.");
    }

    private String normalizeUsername(String value) { return value.trim(); }
    private String normalizeEmail(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private ProfileVerificationException invalidOrExpired() { return new ProfileVerificationException("This verification code has expired. Request a new code."); }
}
