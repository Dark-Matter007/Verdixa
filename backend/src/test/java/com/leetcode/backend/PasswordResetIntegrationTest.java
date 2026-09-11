package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:passwordreset;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc class PasswordResetIntegrationTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired PasswordResetTokenRepository tokens; @Autowired PasswordEncoder encoder;
 @Test void resetIsGenericAndSingleUseAndChangesTheBcryptPassword() throws Exception {
   mvc.perform(post("/api/auth/forgot-password").contentType("application/json").content("{\"email\":\"nobody@example.test\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("If an account exists for that email, a password reset code has been sent."));
   User user=new User("reset-user","reset@example.test",encoder.encode("old-password"),Role.USER); user.setEmailVerified(true); user=users.save(user);
   PasswordResetToken token=new PasswordResetToken(); token.setUser(user); token.setOtpHash(encoder.encode("123456")); token.setExpiresAt(LocalDateTime.now().plusMinutes(5)); token.setResendAvailableAt(LocalDateTime.now()); token.setIssueWindowStartedAt(LocalDateTime.now()); token.setIssueCount(1); tokens.save(token);
   mvc.perform(post("/api/auth/verify-password-reset-otp").contentType("application/json").content("{\"email\":\"reset@example.test\",\"otp\":\"123456\"}")).andExpect(status().isOk());
   mvc.perform(post("/api/auth/reset-password").contentType("application/json").content("{\"email\":\"reset@example.test\",\"otp\":\"123456\",\"password\":\"new-password\",\"confirmPassword\":\"new-password\"}")).andExpect(status().isOk());
   assertThat(encoder.matches("old-password",users.findById(user.getId()).orElseThrow().getPassword())).isFalse(); assertThat(encoder.matches("new-password",users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
   mvc.perform(post("/api/auth/reset-password").contentType("application/json").content("{\"email\":\"reset@example.test\",\"otp\":\"123456\",\"password\":\"another-password\",\"confirmPassword\":\"another-password\"}")).andExpect(status().isBadRequest());
 }
}
