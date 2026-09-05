package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** API-level regression coverage for Prompt 3's safe admin analytics and management endpoints. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:prompt3;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc
class Prompt3AdminApiTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired ProblemRepository problems;
 @Autowired CuratedCollectionRepository collections; @Autowired LearningPathRepository paths; @Autowired DailyChallengeRepository challenges;
 Long userId, problemId;
 @BeforeEach void seed(){ User u=users.findByUsername("prompt3user").orElseGet(()->users.save(new User("prompt3user","prompt3user@test.dev","hash",Role.USER)));userId=u.getId(); Problem p=new Problem();p.setTitle("Prompt three problem "+System.nanoTime());p.setDescription("d");p.setDifficulty("EASY");p.setActive(true);problemId=problems.save(p).getId(); }
 @Test @WithMockUser(roles="ADMIN") void adminUserAnalyticsIsSafe() throws Exception { mvc.perform(get("/api/admin/users/{id}/analytics",userId)).andExpect(status().isOk()).andExpect(jsonPath("$.username").exists()).andExpect(jsonPath("$.solvedTotal").value(0)).andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.recentSubmissions").isArray()); }
 @Test @WithMockUser(roles="USER") void userCannotAccessAdminAnalytics() throws Exception { mvc.perform(get("/api/admin/users/{id}/analytics",userId)).andExpect(status().isForbidden());mvc.perform(get("/api/admin/problems/{id}/analytics",problemId)).andExpect(status().isForbidden()); }
 @Test @WithMockUser(roles="ADMIN") void problemAnalyticsHidesTestcasePayloads() throws Exception { mvc.perform(get("/api/admin/problems/{id}/analytics",problemId)).andExpect(status().isOk()).andExpect(jsonPath("$.executionMode").value("STDIN")).andExpect(jsonPath("$.hiddenTestCases").exists()).andExpect(jsonPath("$.inputData").doesNotExist()).andExpect(jsonPath("$.expectedOutput").doesNotExist()).andExpect(jsonPath("$.functionArguments").doesNotExist()); }
 @Test @WithMockUser(roles="ADMIN") void collectionAndPathMutationsWork() throws Exception { String c="{\"title\":\"Interview\",\"description\":\"x\",\"published\":false}";mvc.perform(post("/api/collections").contentType(MediaType.APPLICATION_JSON).content(c)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Interview")); String p="{\"title\":\"Foundations\",\"description\":\"x\"}";mvc.perform(post("/api/learning-paths").contentType(MediaType.APPLICATION_JSON).content(p)).andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false)); }
 @Test @WithMockUser(roles="ADMIN") void dailyChallengeRejectsDuplicateDate() throws Exception { String body="{\"challengeDate\":\"2030-01-02\",\"problem\":{\"id\":"+problemId+"}}";mvc.perform(post("/api/daily-challenges").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());mvc.perform(post("/api/daily-challenges").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is4xxClientError()); }
 @Test @WithMockUser(username="prompt3user",roles="USER") void todayWithoutScheduleReturnsCleanEmptyState() throws Exception { mvc.perform(get("/api/daily-challenges/today")).andExpect(status().isNoContent()); }
 @Test @WithMockUser(roles="ADMIN") void paginationEnvelopeHasRequiredMetadata() throws Exception { mvc.perform(get("/api/users/page").param("page","0").param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(1)).andExpect(jsonPath("$.totalElements").exists()).andExpect(jsonPath("$.first").value(true));mvc.perform(get("/api/problems/admin/page").param("search","Prompt three").param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));mvc.perform(get("/api/admin/activity/page")).andExpect(status().isOk()).andExpect(jsonPath("$.last").exists()); }
}
