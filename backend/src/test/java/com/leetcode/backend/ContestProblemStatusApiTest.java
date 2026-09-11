package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:conteststatus;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc
class ContestProblemStatusApiTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired ContestRepository contests; @Autowired ProblemRepository problems; @Autowired ContestProblemRepository links; @Autowired ContestRegistrationRepository registrations; @Autowired SubmissionRepository submissions;
 User user; Contest contest; ContestProblem completed, attempted;
 @BeforeEach void setUp(){
  user=users.findByUsername("status-user").orElseGet(()->users.save(new User("status-user","status@example.test","hash",Role.USER)));
  contest=new Contest(); contest.setTitle("Status contest");contest.setSlug("status-"+System.nanoTime());contest.setDescription("x");contest.setStartAt(LocalDateTime.now().minusMinutes(20));contest.setEndAt(LocalDateTime.now().plusMinutes(20));contest.setStatus(ContestStatus.UPCOMING);contest.setVisibility(ContestVisibility.PUBLIC);contest.setCreatedBy(user);contest=contests.save(contest);
  completed=link("Completed"); attempted=link("Attempted"); link("Unopened"); ContestRegistration registration=new ContestRegistration();registration.setContest(contest);registration.setUser(user);registrations.save(registration);
  submission(completed,"ACCEPTED"); submission(attempted,"WRONG_ANSWER");
 }
 @Test @WithMockUser(username="status-user",roles="USER") void exposesContestScopedCompletionStates() throws Exception {mvc.perform(get("/api/contests/{id}/problems",contest.getId())).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("COMPLETED")).andExpect(jsonPath("$[1].status").value("ATTEMPTED")).andExpect(jsonPath("$[2].status").value("UNOPENED"));}
 private ContestProblem link(String title){Problem p=new Problem();p.setTitle(title);p.setDescription("x");p.setDifficulty("EASY");p.setActive(true);p=problems.save(p);ContestProblem cp=new ContestProblem();cp.setContest(contest);cp.setProblem(p);cp.setDisplayOrder(links.findByContestIdOrderByDisplayOrderAsc(contest.getId()).size()+1);cp.setPoints(100);return links.save(cp);}
 private void submission(ContestProblem cp,String status){Submission s=new Submission();s.setUser(user);s.setContest(contest);s.setContestProblem(cp);s.setProblem(cp.getProblem());s.setLanguage("java");s.setSourceCode("class Main{}");s.setStatus(status);s.setOutput("");s.setErrorMessage("");s.setExecutionTimeMs(1L);s.setPassedTestCases(0);s.setTotalTestCases(0);submissions.save(s);}
}
