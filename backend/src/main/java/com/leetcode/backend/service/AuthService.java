package com.leetcode.backend.service;

import com.leetcode.backend.dto.LoginRequest;
import com.leetcode.backend.dto.RegisterRequest;
import com.leetcode.backend.dto.VerifyEmailOtpRequest;
import com.leetcode.backend.model.EmailVerificationToken;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.EmailVerificationTokenRepository;
import com.leetcode.backend.repository.UserRepository;
import com.leetcode.backend.repository.PasswordResetTokenRepository;
import com.leetcode.backend.model.PasswordResetToken;
import com.leetcode.backend.security.JwtService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_MAX_ISSUES_PER_HOUR = 5;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;
    private final PasswordResetTokenRepository resetTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, EmailVerificationTokenRepository tokens, PasswordResetTokenRepository resetTokens, PasswordEncoder passwordEncoder, JwtService jwtService, ApplicationEventPublisher events) {
        this.users = users; this.tokens = tokens; this.resetTokens = resetTokens; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.events = events;
    }

    /** Always returns normally for an existing email to avoid account enumeration. */
    @Transactional public void register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        Optional<User> byEmail = users.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            User user = users.lockById(byEmail.get().getId()).orElseThrow();
            if (!user.isEmailVerified()) issueOtpIfAllowed(user);
            return;
        }
        if (users.existsByUsernameIgnoreCase(request.getUsername().trim())) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim()); user.setEmail(email); user.setPassword(passwordEncoder.encode(request.getPassword())); user.setRole(Role.USER); user.setEmailVerified(false);
        issueOtpIfAllowed(users.save(user));
    }

    /** Generic outcome is intentional, including for an unknown or already verified email. */
    @Transactional public void resendEmailOtp(String requestedEmail) {
        users.findByEmailIgnoreCase(normalizeEmail(requestedEmail)).ifPresent(candidate -> {
            User user = users.lockById(candidate.getId()).orElseThrow();
            if (!user.isEmailVerified()) issueOtpIfAllowed(user);
        });
    }

    @Transactional(noRollbackFor = OtpVerificationException.class) public void verifyEmailOtp(VerifyEmailOtpRequest request) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(request.email())).flatMap(candidate -> users.lockById(candidate.getId())).orElseThrow(OtpVerificationException::new);
        if (user.isEmailVerified()) throw new OtpVerificationException();
        EmailVerificationToken token = tokens.findByUserIdForUpdate(user.getId()).orElseThrow(OtpVerificationException::new);
        LocalDateTime now = LocalDateTime.now();
        if (token.getConsumedAt() != null || !now.isBefore(token.getExpiresAt()) || token.getFailedAttempts() >= OTP_MAX_ATTEMPTS) throw new OtpVerificationException();
        if (!passwordEncoder.matches(request.otp(), token.getOtpHash())) {
            token.setFailedAttempts(token.getFailedAttempts() + 1); tokens.save(token); throw new OtpVerificationException();
        }
        token.setConsumedAt(now); user.setEmailVerified(true); users.save(user); tokens.save(token);
        events.publishEvent(new EmailNotificationEvents.AccountVerified(user.getEmail(), user.getUsername()));
    }

    public User authenticate(LoginRequest request) {
        User user = users.findByUsernameIgnoreCase(request.getUsername().trim()).orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) throw new IllegalArgumentException("This account does not have a local password. Use a connected sign-in method or reset your password.");
        if (!user.isEmailVerified()) throw new EmailNotVerifiedException();
        return user;
    }
    public String generateToken(User user) { return jwtService.generateToken(user); }

    /** Always succeeds publicly to avoid revealing whether an address owns an account. */
    @Transactional public void requestPasswordReset(String requestedEmail) {
        users.findByEmailIgnoreCase(normalizeEmail(requestedEmail)).ifPresent(candidate -> {
            User user = users.lockById(candidate.getId()).orElseThrow();
            issuePasswordResetIfAllowed(user);
        });
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public void verifyPasswordResetOtp(String requestedEmail, String otp) {
        PasswordResetToken token = resetToken(requestedEmail);
        validateResetToken(token, otp, false);
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public void resetPassword(String requestedEmail, String otp, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("Passwords do not match.");
        PasswordResetToken token = resetToken(requestedEmail);
        validateResetToken(token, otp, true);
        User user = users.lockById(token.getUser().getId()).orElseThrow(OtpVerificationException::new);
        user.setPassword(passwordEncoder.encode(password));
        user.incrementAuthVersion();
        token.setConsumedAt(LocalDateTime.now());
        users.save(user); resetTokens.save(token);
    }

    @Transactional public void resendPasswordReset(String requestedEmail) { requestPasswordReset(requestedEmail); }

    private PasswordResetToken resetToken(String requestedEmail) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(requestedEmail)).flatMap(candidate -> users.lockById(candidate.getId())).orElseThrow(OtpVerificationException::new);
        return resetTokens.findByUserIdForUpdate(user.getId()).orElseThrow(OtpVerificationException::new);
    }
    private void validateResetToken(PasswordResetToken token, String otp, boolean consume) {
        LocalDateTime now = LocalDateTime.now();
        if (token.getConsumedAt() != null || !now.isBefore(token.getExpiresAt()) || token.getFailedAttempts() >= OTP_MAX_ATTEMPTS) throw new OtpVerificationException();
        if (!passwordEncoder.matches(otp, token.getOtpHash())) { token.setFailedAttempts(token.getFailedAttempts()+1); resetTokens.save(token); throw new OtpVerificationException(); }
        if (consume) token.setConsumedAt(now);
    }
    private void issuePasswordResetIfAllowed(User user) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = resetTokens.findByUserIdForUpdate(user.getId()).orElse(null);
        if (token != null && token.getResendAvailableAt().isAfter(now)) return;
        if (token == null) { token = new PasswordResetToken(); token.setUser(user); token.setIssueWindowStartedAt(now); token.setIssueCount(0); }
        else if (!token.getIssueWindowStartedAt().plusHours(1).isAfter(now)) { token.setIssueWindowStartedAt(now); token.setIssueCount(0); }
        if (token.getIssueCount() >= OTP_MAX_ISSUES_PER_HOUR) return;
        String otp = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        token.setOtpHash(passwordEncoder.encode(otp)); token.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES)); token.setResendAvailableAt(now.plusSeconds(OTP_RESEND_COOLDOWN_SECONDS)); token.setFailedAttempts(0); token.setConsumedAt(null); token.setIssueCount(token.getIssueCount()+1);
        resetTokens.save(token); events.publishEvent(new EmailNotificationEvents.PasswordResetOtpIssued(user.getEmail(), user.getUsername(), otp));
    }

    private void issueOtpIfAllowed(User user) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationToken token = tokens.findByUserIdForUpdate(user.getId()).orElse(null);
        if (token != null && token.getResendAvailableAt().isAfter(now)) return;
        if (token == null) { token = new EmailVerificationToken(); token.setUser(user); token.setIssueWindowStartedAt(now); token.setIssueCount(0); }
        else if (!token.getIssueWindowStartedAt().plusHours(1).isAfter(now)) { token.setIssueWindowStartedAt(now); token.setIssueCount(0); }
        if (token.getIssueCount() >= OTP_MAX_ISSUES_PER_HOUR) return;
        String otp = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        token.setOtpHash(passwordEncoder.encode(otp)); token.setIssuedAt(now); token.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES)); token.setResendAvailableAt(now.plusSeconds(OTP_RESEND_COOLDOWN_SECONDS)); token.setFailedAttempts(0); token.setConsumedAt(null); token.setIssueCount(token.getIssueCount() + 1);
        tokens.save(token);
        events.publishEvent(new EmailNotificationEvents.OtpIssued(user.getEmail(), user.getUsername(), otp));
    }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
