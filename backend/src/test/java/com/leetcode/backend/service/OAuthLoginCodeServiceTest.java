package com.leetcode.backend.service;

import com.leetcode.backend.dto.AuthResponse;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.OAuthLoginCodeRepository;
import com.leetcode.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthLoginCodeServiceTest {
    @Mock OAuthLoginCodeRepository codes;
    @Mock JwtService jwtService;
    @InjectMocks OAuthLoginCodeService service;

    @Test void issuedCodeExchangesOnceForTheNormalVerdixaJwt() {
        User user = new User("ada", "ada@example.com", null, Role.USER); user.setEmailVerified(true);
        ArgumentCaptor<OAuthLoginCode> saved = ArgumentCaptor.forClass(OAuthLoginCode.class);
        when(codes.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        String browserCode = service.create(user);
        OAuthLoginCode entry = saved.getValue();
        assertNotEquals(browserCode, entry.getCodeHash()); assertTrue(entry.getExpiresAt().isAfter(LocalDateTime.now()));
        when(codes.findByCodeHashForUpdate(anyString())).thenReturn(Optional.of(entry));
        when(jwtService.generateToken(user)).thenReturn("verdixa-jwt");

        AuthResponse response = service.exchange(browserCode);

        assertEquals("verdixa-jwt", response.getToken()); assertNotNull(entry.getConsumedAt());
        assertThrows(IllegalArgumentException.class, () -> service.exchange(browserCode));
    }
}
