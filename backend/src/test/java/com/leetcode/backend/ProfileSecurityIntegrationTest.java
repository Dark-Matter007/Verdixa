package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import com.leetcode.backend.security.JwtService;
import com.leetcode.backend.service.EmailService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.leetcode.backend.model.AccountOtpPurpose.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profilesecurity;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"
})
@AutoConfigureMockMvc
class ProfileSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountSecurityOtpRepository otps;
    @Autowired UserOAuthAccountRepository oauthAccounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @MockitoBean EmailService email;

    @BeforeEach void clean() { otps.deleteAll(); oauthAccounts.deleteAll(); users.deleteAll(); }

    @Test void usernameOtpIsAuthenticatedHashedSingleUseAndInvalidatesTheOldJwt() throws Exception {
        User user = saveUser("ada", "ada@example.test", "old-password", Role.USER);
        String token = jwt.generateToken(user);
        mvc.perform(post("/api/profile/username/change/request").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token)).content("{\"username\":\"ada_lovelace\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("VERIFY_USERNAME"));
        AccountSecurityOtp otp = otp(user, USERNAME_CHANGE);
        assertThat(otp.getOtpHash()).doesNotContain("123456");

        mvc.perform(post("/api/profile/username/change/verify").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token)).content("{\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("The verification code is incorrect."));
        otp = otp(user, USERNAME_CHANGE); otp.setOtpHash(encoder.encode("123456")); otps.save(otp);
        mvc.perform(post("/api/profile/username/change/verify").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token)).content("{\"otp\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logoutRequired").value(true));

        User changed = users.findById(user.getId()).orElseThrow();
        assertThat(changed.getUsername()).isEqualTo("ada_lovelace"); assertThat(changed.getRole()).isEqualTo(Role.USER); assertThat(changed.getAuthVersion()).isEqualTo(1);
        mvc.perform(get("/api/profile").header("Authorization", bearer(token))).andExpect(status().isForbidden());
        mvc.perform(post("/api/profile/username/change/verify").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bearer(jwt.generateToken(changed))).content("{\"otp\":\"123456\"}")).andExpect(status().isBadRequest());
    }

    @Test void usernameValidationRejectsUnauthenticatedInvalidDuplicateAndSupportsAdmin() throws Exception {
        saveUser("existing", "existing@example.test", "password", Role.USER);
        User admin = saveUser("admin", "admin@example.test", "password", Role.ADMIN);
        mvc.perform(post("/api/profile/username/change/request").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"new_name\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/profile/username/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(jwt.generateToken(admin))).content("{\"username\":\"bad name\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/profile/username/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(jwt.generateToken(admin))).content("{\"username\":\"EXISTING\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("That username is already in use."));
        mvc.perform(post("/api/profile/username/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(jwt.generateToken(admin))).content("{\"username\":\"admin_secure\"}"))
                .andExpect(status().isOk());
        AccountSecurityOtp otp = otp(admin, USERNAME_CHANGE); otp.setOtpHash(encoder.encode("123456")); otps.save(otp);
        mvc.perform(post("/api/profile/username/change/verify").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(jwt.generateToken(admin))).content("{\"otp\":\"123456\"}"))
                .andExpect(status().isOk());
        assertThat(users.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test void expiredUsernameOtpCannotBeUsed() throws Exception {
        User user = saveUser("expired", "expired@example.test", "password", Role.USER); String token = jwt.generateToken(user);
        mvc.perform(post("/api/profile/username/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"username\":\"expired_new\"}"))
                .andExpect(status().isOk());
        AccountSecurityOtp otp = otp(user, USERNAME_CHANGE); otp.setOtpHash(encoder.encode("123456")); otp.setExpiresAt(LocalDateTime.now().minusSeconds(1)); otps.save(otp);
        mvc.perform(post("/api/profile/username/change/verify").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("This verification code has expired. Request a new code."));
        assertThat(users.findById(user.getId()).orElseThrow().getUsername()).isEqualTo("expired");
    }

    @Test void emailChangesOnlyAfterCurrentAndNewAddressAreVerified() throws Exception {
        User user = saveUser("email_user", "old@example.test", "old-password", Role.USER); String token = jwt.generateToken(user);
        saveUser("taken", "taken@example.test", "password", Role.USER);
        mvc.perform(post("/api/profile/email/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"newEmail\":\"bad\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/profile/email/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"newEmail\":\"TAKEN@example.test\",\"currentPassword\":\"old-password\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("This email address is already associated with another account."));
        mvc.perform(post("/api/profile/email/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"newEmail\":\"new@example.test\",\"currentPassword\":\"wrong-password\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Current password is incorrect."));

        mvc.perform(post("/api/profile/email/change/request").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"newEmail\":\" New@Example.Test \",\"currentPassword\":\"old-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("VERIFY_CURRENT_EMAIL"));
        assertThat(users.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("old@example.test");
        AccountSecurityOtp current = otp(user, EMAIL_CHANGE_CURRENT); current.setOtpHash(encoder.encode("123456")); otps.save(current);
        mvc.perform(post("/api/profile/email/change/verify-current").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"otp\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("VERIFY_NEW_EMAIL"));
        mvc.perform(post("/api/profile/email/change/verify-current").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("This verification code has expired. Request a new code."));
        assertThat(users.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("old@example.test");
        AccountSecurityOtp next = otp(user, EMAIL_CHANGE_NEW); next.setOtpHash(encoder.encode("654321")); otps.save(next);
        mvc.perform(post("/api/profile/email/change/verify-new").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/profile/email/change/verify-new").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"otp\":\"654321\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logoutRequired").value(true));
        User changed = users.findById(user.getId()).orElseThrow(); assertThat(changed.getEmail()).isEqualTo("new@example.test"); assertThat(changed.isEmailVerified()).isTrue(); assertThat(changed.getAuthVersion()).isEqualTo(1);
        verify(email).sendEmailChangedNotice("old@example.test", "email_user", "new@example.test");
    }

    @Test void passwordChangeRequiresCurrentPasswordHashesReplacementAndPreservesRole() throws Exception {
        User user = saveUser("password_user", "password@example.test", "old-password", Role.ADMIN); String token = jwt.generateToken(user);
        mvc.perform(post("/api/profile/password/change").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"new-password\",\"confirmPassword\":\"new-password\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Current password is incorrect."));
        mvc.perform(post("/api/profile/password/change").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\",\"confirmPassword\":\"mismatch\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/profile/password/change").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\",\"confirmPassword\":\"new-password\"}"))
                .andExpect(status().isOk());
        User changed = users.findById(user.getId()).orElseThrow(); assertThat(changed.getPassword()).doesNotContain("new-password"); assertThat(encoder.matches("old-password", changed.getPassword())).isFalse(); assertThat(encoder.matches("new-password", changed.getPassword())).isTrue(); assertThat(changed.getRole()).isEqualTo(Role.ADMIN); assertThat(changed.getAuthVersion()).isEqualTo(1);
    }

    @Test void oauthOnlyPasswordSetupDoesNotCorruptProviderIdentity() throws Exception {
        User user = saveUser("oauth_user", "oauth@example.test", null, Role.USER);
        UserOAuthAccount account = new UserOAuthAccount(); account.setUser(user); account.setProvider(OAuthProvider.GOOGLE); account.setProviderUserId("stable-google-subject"); account.setProviderEmail("provider@example.test"); account.setCreatedAt(LocalDateTime.now()); account.setUpdatedAt(LocalDateTime.now()); oauthAccounts.save(account);
        String token = jwt.generateToken(user);
        mvc.perform(get("/api/profile").header("Authorization", bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.hasLocalPassword").value(false)).andExpect(jsonPath("$.connectedProviders[0]").value("GOOGLE"));
        mvc.perform(post("/api/profile/password/setup/request").header("Authorization", bearer(token))).andExpect(status().isOk());
        AccountSecurityOtp otp = otp(user, PASSWORD_SETUP); otp.setOtpHash(encoder.encode("123456")); otps.save(otp);
        mvc.perform(post("/api/profile/password/setup/verify").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"otp\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("SET_PASSWORD"));
        mvc.perform(post("/api/profile/password/setup").contentType(MediaType.APPLICATION_JSON).header("Authorization", bearer(token)).content("{\"newPassword\":\"local-password\",\"confirmPassword\":\"local-password\"}"))
                .andExpect(status().isOk());
        assertThat(encoder.matches("local-password", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
        UserOAuthAccount unchanged = oauthAccounts.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "stable-google-subject").orElseThrow(); assertThat(unchanged.getProviderEmail()).isEqualTo("provider@example.test");
    }

    private User saveUser(String username, String emailAddress, String password, Role role) {
        User user = new User(username, emailAddress, password == null ? null : encoder.encode(password), role); user.setEmailVerified(true); return users.saveAndFlush(user);
    }
    private AccountSecurityOtp otp(User user, AccountOtpPurpose purpose) { return otps.findByUserIdAndPurpose(user.getId(), purpose).orElseThrow(); }
    private String bearer(String token) { return "Bearer " + token; }
}
