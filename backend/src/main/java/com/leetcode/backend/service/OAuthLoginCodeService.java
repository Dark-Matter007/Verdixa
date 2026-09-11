package com.leetcode.backend.service;

import com.leetcode.backend.dto.AuthResponse;
import com.leetcode.backend.model.OAuthLoginCode;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.OAuthLoginCodeRepository;
import com.leetcode.backend.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class OAuthLoginCodeService {
    private static final int CODE_LIFETIME_SECONDS = 90;
    private final OAuthLoginCodeRepository codes; private final JwtService jwtService; private final SecureRandom random = new SecureRandom();
    public OAuthLoginCodeService(OAuthLoginCodeRepository codes, JwtService jwtService) { this.codes = codes; this.jwtService = jwtService; }

    @Transactional
    public String create(User user) {
        byte[] value = new byte[32]; random.nextBytes(value);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        OAuthLoginCode entry = new OAuthLoginCode(); entry.setUser(user); entry.setCodeHash(hash(code)); entry.setExpiresAt(LocalDateTime.now().plusSeconds(CODE_LIFETIME_SECONDS));
        codes.save(entry); return code;
    }

    @Transactional
    public AuthResponse exchange(String code) {
        OAuthLoginCode entry = codes.findByCodeHashForUpdate(hash(code)).orElseThrow(() -> new IllegalArgumentException("Your sign-in session expired. Please try again."));
        if (entry.getConsumedAt() != null || !LocalDateTime.now().isBefore(entry.getExpiresAt())) throw new IllegalArgumentException("Your sign-in session expired. Please try again.");
        entry.setConsumedAt(LocalDateTime.now()); codes.save(entry);
        User user = entry.getUser();
        return new AuthResponse("Login successful", user.getUsername(), user.getRole().name(), jwtService.generateToken(user.getUsername(), user.getRole().name()));
    }

    private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
}
