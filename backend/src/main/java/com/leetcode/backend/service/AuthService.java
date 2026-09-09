package com.leetcode.backend.service;

import com.leetcode.backend.dto.LoginRequest;
import com.leetcode.backend.dto.RegisterRequest;
import com.leetcode.backend.dto.VerifyEmailOtpRequest;
import com.leetcode.backend.model.EmailVerificationToken;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.EmailVerificationTokenRepository;
import com.leetcode.backend.repository.UserRepository;
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
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, EmailVerificationTokenRepository tokens, PasswordEncoder passwordEncoder, JwtService jwtService, ApplicationEventPublisher events) {
        this.users = users; this.tokens = tokens; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.events = events;
    }

    /** Always returns normally for an existing email to avoid account enumeration. */
    @Transactional public void register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        Optional<User> byEmail = users.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = users.lockById(byEmail.get().getId()).orElseThrow();
            if (!user.isEmailVerified()) issueOtpIfAllowed(user);
            return;
        }
        if (users.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim()); user.setEmail(email); user.setPassword(passwordEncoder.encode(request.getPassword())); user.setRole(Role.USER); user.setEmailVerified(false);
        issueOtpIfAllowed(users.save(user));
    }

    /** Generic outcome is intentional, including for an unknown or already verified email. */
    @Transactional public void resendEmailOtp(String requestedEmail) {
        users.findByEmail(normalizeEmail(requestedEmail)).ifPresent(candidate -> {
            User user = users.lockById(candidate.getId()).orElseThrow();
            if (!user.isEmailVerified()) issueOtpIfAllowed(user);
        });
    }

    @Transactional(noRollbackFor = OtpVerificationException.class) public void verifyEmailOtp(VerifyEmailOtpRequest request) {
        User user = users.findByEmail(normalizeEmail(request.email())).flatMap(candidate -> users.lockById(candidate.getId())).orElseThrow(OtpVerificationException::new);
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
        User user = users.findByUsername(request.getUsername().trim()).orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) throw new IllegalArgumentException("Invalid username or password.");
        if (!user.isEmailVerified()) throw new EmailNotVerifiedException();
        return user;
    }
    public String generateToken(User user) { return jwtService.generateToken(user.getUsername(), user.getRole().name()); }

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
