package com.leetcode.backend;

import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.UserRepository;
import com.leetcode.backend.service.AssessmentCreatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:creatordoc;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters",
        "verdixa.verification.storage-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=",
        "verdixa.verification.storage-path=${java.io.tmpdir}/verdixa-creator-document-security-test"
})
@AutoConfigureMockMvc
class AssessmentCreatorDocumentSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AssessmentCreatorService creators;
    private Long applicationId;

    @BeforeEach
    void setUp() {
        User creator = users.findByUsername("document-owner").orElseGet(() -> {
            User user = new User("document-owner", "document-owner@example.test", "hash", Role.USER);
            user.setEmailVerified(true);
            return users.save(user);
        });
        Map<String, Object> application = creators.status(creator.getUsername());
        if ("NONE".equals(application.get("status"))) {
            byte[] pdf = "%PDF-1.7 private document".getBytes();
            application = creators.apply(creator.getUsername(), "Document Owner", "Verdixa", "Engineer",
                    "Hiring", null, null, "Candidate assessments", "IN", "Certificate of incorporation",
                    new MockMultipartFile("document", "identity.pdf", "application/pdf", pdf));
        }
        applicationId = ((Number) application.get("id")).longValue();
    }

    @Test
    @WithMockUser(username = "normal-user", roles = "USER")
    void normalUserCannotAccessAdminCreatorApplications() throws Exception {
        mvc.perform(get("/api/admin/assessment-creators")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "normal-user", roles = "USER")
    void normalUserCannotDownloadVerificationDocument() throws Exception {
        mvc.perform(get("/api/admin/assessment-creators/{id}/document", applicationId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "creator-admin", roles = "ADMIN")
    void adminCanRetrieveDecryptedVerificationDocument() throws Exception {
        mvc.perform(get("/api/admin/assessment-creators/{id}/document", applicationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes("%PDF-1.7 private document".getBytes()));
    }

    @Test
    @WithMockUser(username = "multipart-creator", roles = "USER")
    void creatorApplicationAcceptsBrowserStyleMultipartJpegUpload() throws Exception {
        User user = new User("multipart-creator", "multipart-creator@example.test", "hash", Role.USER);
        user.setEmailVerified(true);
        users.save(user);
        MockMultipartFile document = new MockMultipartFile(
                "document", "identity.jpeg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});

        mvc.perform(multipart("/api/assessment-creator/applications")
                        .file(document)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("fullName", "Multipart Creator")
                        .param("organization", "Verdixa")
                        .param("roleTitle", "Engineer")
                        .param("purpose", "Technical evaluation")
                        .param("intendedUse", "Candidate assessments")
                        .param("country", "IN")
                        .param("documentType", "Passport"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EMAIL_OTP_PENDING"));
    }

    @Test
    void studentAndEmployeeProofTypesRemainRejected() {
        User user = new User("personal-proof-user", "personal-proof@example.test", "hash", Role.USER);
        user.setEmailVerified(true);
        users.save(user);

        assertThrows(IllegalArgumentException.class, () -> creators.apply(
                user.getUsername(), "Personal Proof User", "Verdixa", "Engineer", "Hiring", null, null,
                "Candidate assessments", "IN", "Employee ID",
                new MockMultipartFile("document", "passport.pdf", "application/pdf", "%PDF-1.7".getBytes())));
    }
}
