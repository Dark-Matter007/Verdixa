package com.leetcode.backend;

import com.leetcode.backend.dto.HintListResponse;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import com.leetcode.backend.service.HintService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:hints;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc
class HintProgressionIntegrationTest {
    @Autowired HintService service;
    @Autowired UserRepository users;
    @Autowired ProblemRepository problems;
    @Autowired ProblemHintRepository hints;
    @Autowired HintRevealRepository reveals;
    @Autowired SubmissionRepository submissions;
    @Autowired MockMvc mvc;
    User user;
    Problem problem;
    List<ProblemHint> orderedHints;

    @BeforeEach
    void seed() {
        String unique = Long.toUnsignedString(System.nanoTime());
        user = users.save(new User("hint-user-" + unique, unique + "@test.dev", "hash", Role.USER));
        problem = problem("Hint progression " + unique);
        orderedHints = List.of(hint(1), hint(2), hint(3));
    }

    @Test
    void hintsUnlockAtThreeAttemptMilestonesWithoutLeakingContent() {
        assertProgress(0, false, false, false);
        attempts(user, problem, 2); assertProgress(2, false, false, false);
        attempts(user, problem, 1); assertProgress(3, true, false, false);
        attempts(user, problem, 2); assertProgress(5, true, false, false);
        attempts(user, problem, 1); assertProgress(6, true, true, false);
        attempts(user, problem, 3); assertProgress(9, true, true, true);
    }

    @Test
    void revealIsPersistentIdempotentAndIsolatedByUser() {
        attempts(user, problem, 3);
        var revealed = service.reveal(problem.getId(), orderedHints.get(0).getId(), user.getUsername());
        assertTrue(revealed.revealed()); assertEquals("Title 1", revealed.title());
        service.reveal(problem.getId(), orderedHints.get(0).getId(), user.getUsername());
        assertEquals(1, reveals.findByUserIdAndProblemId(user.getId(), problem.getId()).size());

        String unique = Long.toUnsignedString(System.nanoTime());
        User other = users.save(new User("other-" + unique, "other-" + unique + "@test.dev", "hash", Role.USER));
        attempts(other, problem, 3);
        HintListResponse otherState = service.userHints(problem.getId(), other.getUsername());
        assertFalse(otherState.hints().get(0).revealed());
        assertNull(otherState.hints().get(0).title());
    }

    @Test
    void attemptsAreIsolatedByProblem() {
        Problem otherProblem = problem("Other problem " + System.nanoTime());
        attempts(user, otherProblem, 9);
        assertFalse(service.userHints(problem.getId(), user.getUsername()).hints().get(0).available());
    }

    @Test
    @WithMockUser(username="hint-api-user", roles="USER")
    void revealEndpointIsAUserWriteNotAnAdminWrite() throws Exception {
        User apiUser = users.save(new User("hint-api-user", "hint-api-" + System.nanoTime() + "@test.dev", "hash", Role.USER));
        attempts(apiUser, problem, 3);
        mvc.perform(post("/api/problems/{problemId}/hints/{hintId}/reveal", problem.getId(), orderedHints.get(0).getId()))
                .andExpect(status().isOk());
    }

    private void assertProgress(long expectedAttempts, boolean first, boolean second, boolean third) {
        HintListResponse state = service.userHints(problem.getId(), user.getUsername());
        assertEquals(expectedAttempts, state.attempts());
        assertEquals(List.of(first, second, third), state.hints().stream().map(h -> h.available()).toList());
        assertEquals(List.of(3, 6, 9), state.hints().stream().map(h -> h.attemptsRequired()).toList());
        state.hints().stream().filter(h -> !h.revealed()).forEach(h -> {
            assertNull(h.title()); assertNull(h.content());
        });
    }

    private Problem problem(String title) {
        Problem value = new Problem(); value.setTitle(title); value.setDescription("description");
        value.setDifficulty("EASY"); value.setActive(true); return problems.save(value);
    }
    private ProblemHint hint(int order) {
        ProblemHint value = new ProblemHint(); value.setProblem(problem); value.setTitle("Title " + order);
        value.setContent("Secret " + order); value.setDisplayOrder(order); value.setActive(true);
        return hints.save(value);
    }
    private void attempts(User owner, Problem target, int count) {
        for (int index = 0; index < count; index++) {
            Submission value = new Submission(); value.setUser(owner); value.setProblem(target); value.setSourceCode("code");
            value.setLanguage("java"); value.setStatus("WRONG_ANSWER"); submissions.save(value);
        }
        submissions.flush();
    }
}
