package com.leetcode.backend;

import com.leetcode.backend.model.EmailVerificationToken;
import com.leetcode.backend.repository.EmailVerificationTokenRepository;
import com.leetcode.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:emailverification;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"
})
@AutoConfigureMockMvc
class EmailVerificationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository tokens;
    @Autowired PasswordEncoder encoder;

    @Test
    void registrationRequiresHashedSingleUseOtpBeforeLogin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"username\":\"email-flow-user\",\"email\":\"Email.Flow@example.test\",\"password\":\"secure-password\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("If an account requires verification, a code has been sent."));

        var user = users.findByUsername("email-flow-user").orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
        EmailVerificationToken token = tokens.findByUserId(user.getId()).orElseThrow();
        assertThat(token.getOtpHash()).doesNotContain("123456");

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"email-flow-user\",\"password\":\"secure-password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));

        token.setOtpHash(encoder.encode("123456")); tokens.save(token);
        mvc.perform(post("/api/auth/verify-email-otp").contentType("application/json")
                        .content("{\"email\":\"email.flow@example.test\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account verified successfully. You can now sign in."));
        assertThat(users.findById(user.getId()).orElseThrow().isEmailVerified()).isTrue();

        mvc.perform(post("/api/auth/verify-email-otp").contentType("application/json")
                        .content("{\"email\":\"email.flow@example.test\",\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"email-flow-user\",\"password\":\"secure-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void resendForUnknownEmailHasTheSameGenericResponse() throws Exception {
        mvc.perform(post("/api/auth/resend-email-otp").contentType("application/json")
                        .content("{\"email\":\"nobody@example.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account requires verification, a code has been sent."));
    }
}
