package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:daily;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc
class DailyChallengeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DailyChallengeRepository challenges;
    @Autowired ProblemRepository problems;
    @Autowired UserRepository users;
    Problem problem;

    @BeforeEach
    void seed() {
        challenges.deleteAll();
        if (users.findByUsername("daily-user").isEmpty()) {
            users.save(new User("daily-user", "daily-user@test.dev", "hash", Role.USER));
        }
        problem = new Problem(); problem.setTitle("Daily DTO problem " + System.nanoTime());
        problem.setDescription("Real daily data"); problem.setDifficulty("MEDIUM");
        problem.setTags("arrays,hashing"); problem.setActive(true); problem = problems.save(problem);
    }

    @Test
    @WithMockUser(username="daily-user", roles="USER")
    void currentDateChallengeUsesSafeDtoMapping() throws Exception {
        schedule(LocalDate.now(ZoneId.systemDefault()), problem);
        mvc.perform(get("/api/daily-challenges/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeDate").value(LocalDate.now(ZoneId.systemDefault()).toString()))
                .andExpect(jsonPath("$.problem.title").value(problem.getTitle()))
                .andExpect(jsonPath("$.problem.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.problem.tags").value("arrays,hashing"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.problem.functionSignature").doesNotExist());
    }

    @Test
    @WithMockUser(username="daily-user", roles="USER")
    void noCurrentChallengeIsNoContentNotAServerError() throws Exception {
        mvc.perform(get("/api/daily-challenges/today")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username="daily-user", roles="USER")
    void tomorrowChallengeIsNotSelectedAsToday() throws Exception {
        schedule(LocalDate.now(ZoneId.systemDefault()).plusDays(1), problem);
        mvc.perform(get("/api/daily-challenges/today")).andExpect(status().isNoContent());
    }

    @Test
    void anonymousUserCannotReadDailyChallenge() throws Exception {
        mvc.perform(get("/api/daily-challenges/today")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="daily-user", roles="USER")
    void userCannotUseDailyAdminEndpoint() throws Exception {
        mvc.perform(get("/api/daily-challenges/admin")).andExpect(status().isForbidden());
    }

    private void schedule(LocalDate date, Problem selected) {
        DailyChallenge challenge = new DailyChallenge(); challenge.setChallengeDate(date); challenge.setProblem(selected);
        challenges.saveAndFlush(challenge);
    }
}
