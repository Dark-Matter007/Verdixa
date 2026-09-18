package com.leetcode.backend;

import com.leetcode.backend.model.Assessment;
import com.leetcode.backend.model.AssessmentAccess;
import com.leetcode.backend.model.AssessmentEnums;
import com.leetcode.backend.model.AssessmentSession;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.AssessmentAccessRepository;
import com.leetcode.backend.repository.AssessmentRepository;
import com.leetcode.backend.repository.AssessmentSessionRepository;
import com.leetcode.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assessment-lazy;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters",
        "verdixa.mail.enabled=false"
})
@AutoConfigureMockMvc
class AssessmentAccessLazyLoadingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AssessmentRepository assessments;
    @Autowired AssessmentAccessRepository accesses;
    @Autowired AssessmentSessionRepository sessions;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired EntityManager entityManager;

    @Test
    void detachedParticipantProxyCanAuthorizePreflightAndFirstHeartbeat() throws Exception {
        String grant = "grant-" + UUID.randomUUID();
        long[] ids = new long[2];
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            User host = users.save(new User("lazy-host", "lazy-host@example.com", encoder.encode("password"), Role.USER));
            User participant = users.save(new User("lazy-participant", "lazy-participant@example.com", encoder.encode("password"), Role.ASSESSMENT_GUEST));

            Assessment assessment = new Assessment();
            assessment.setCreator(host);
            assessment.setTitle("Detached proxy assessment");
            assessment.setOrganization("Verdixa QA");
            assessment.setVisibility(AssessmentEnums.Visibility.PUBLIC);
            assessment.setStatus(AssessmentEnums.Status.PUBLISHED);
            assessment.setStartAt(LocalDateTime.now().minusMinutes(1));
            assessment.setEndAt(LocalDateTime.now().plusHours(1));
            assessment.setInviteToken(UUID.randomUUID().toString().replace("-", ""));
            assessment = assessments.save(assessment);

            AssessmentAccess access = new AssessmentAccess();
            access.setAssessment(assessment);
            access.setParticipant(participant);
            access.setEmail(participant.getEmail());
            access.setFullName("Lazy Participant");
            access.setVerifiedAt(LocalDateTime.now());
            access.setGrantHash(encoder.encode(grant));
            access.setGrantExpiresAt(assessment.getEndAt());
            accesses.save(access);

            AssessmentSession session = new AssessmentSession();
            session.setAssessment(assessment);
            session.setParticipant(participant);
            session.setStartedAt(LocalDateTime.now());
            session.setLastHeartbeatAt(LocalDateTime.now());
            session = sessions.save(session);
            ids[0] = assessment.getId();
            ids[1] = session.getId();
            entityManager.flush();
            entityManager.clear();
        });

        mvc.perform(post("/api/assessment-sessions/{sessionId}/heartbeat", ids[1])
                        .header("X-Assessment-Access", grant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(get("/api/assessment-access/{assessmentId}/status", ids[0])
                        .header("X-Assessment-Access", grant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessment.hostDisplayName").value("lazy-host"))
                .andExpect(jsonPath("$.assessment.participantEligible").value(true));
    }
}
