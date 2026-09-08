package com.leetcode.backend;

import com.leetcode.backend.dto.*;
import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import com.leetcode.backend.service.*;
import com.leetcode.backend.config.EditorialCatalogBackfill;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:milestones;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc
class EditorialCertificateIntegrationTest {
 @Autowired UserRepository users; @Autowired ProblemRepository problems; @Autowired SubmissionRepository submissions;
 @Autowired EditorialRepository editorials; @Autowired EditorialRevealRepository reveals; @Autowired UserCertificateRepository certificates;
 @Autowired EditorialAccessService access; @Autowired CertificateService awards; @Autowired EditorialCatalogBackfill backfill;
 @Autowired MockMvc mvc;
 User current;Problem problem;
 @BeforeEach void setup(){current=newUser();problem=newProblem();Editorial e=new Editorial();e.setProblem(problem);e.setTitle("Reviewed");e.setPublished(true);e.setApproach("SECRET APPROACH");e.setCorrectness("SECRET PROOF");e.setPythonSolution("SECRET CODE");editorials.save(e);}
 User newUser(){String name="milestone-"+UUID.randomUUID();return users.save(new User(name,name+"@test.dev","hash",Role.USER));}
 Problem newProblem(){Problem p=new Problem();p.setTitle("Fixture "+UUID.randomUUID());p.setDescription("Fixture");p.setDifficulty("EASY");return problems.save(p);}
 void submission(User u,Problem p,String status){Submission s=new Submission();s.setUser(u);s.setProblem(p);s.setStatus(status);s.setLanguage("python");s.setSourceCode("return 0");submissions.save(s);}
 void failures(int count){for(int i=0;i<count;i++)submission(current,problem,"WRONG_ANSWER");}
 @Test void lockedAtZeroAndTwoAvailableAtThreeWithoutContent() throws Exception {
  assertEquals("LOCKED",access.access(problem.getId(),current.getUsername()).status());failures(2);
  var two=access.access(problem.getId(),current.getUsername());assertEquals("LOCKED",two.status());assertEquals(2,two.failedSubmissionCount());assertNull(two.editorial());
  mvc.perform(post("/api/problems/{id}/editorial/reveal",problem.getId()).with(user(current.getUsername()).roles("USER"))).andExpect(status().isForbidden());
  failures(1);var three=access.access(problem.getId(),current.getUsername());assertEquals("AVAILABLE",three.status());assertNull(three.editorial());
  mvc.perform(get("/api/problems/{id}/editorial",problem.getId()).with(user(current.getUsername()).roles("USER"))).andExpect(status().isOk()).andExpect(jsonPath("$.editorial").doesNotExist()).andExpect(jsonPath("$.approach").doesNotExist()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET"))));
  mvc.perform(post("/api/problems/{id}/editorial/reveal",problem.getId()).with(user(current.getUsername()).roles("USER"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVEALED")).andExpect(jsonPath("$.editorial.approach").value("SECRET APPROACH"));
  access.reveal(problem.getId(),current.getUsername());assertEquals(1,reveals.countByUserIdAndProblemId(current.getId(),problem.getId()));
  assertEquals("REVEALED",access.access(problem.getId(),current.getUsername()).status());
 }
 @Test void acceptedRequiresExplicitRevealAndUnpublishedNeverLeaks(){submission(current,problem,"ACCEPTED");assertEquals("AVAILABLE",access.access(problem.getId(),current.getUsername()).status());assertNull(access.access(problem.getId(),current.getUsername()).editorial());assertEquals("REVEALED",access.reveal(problem.getId(),current.getUsername()).status());Editorial e=editorials.findByProblemId(problem.getId()).orElseThrow();e.setPublished(false);editorials.save(e);assertNull(access.access(problem.getId(),current.getUsername()).editorial());assertThrows(RuntimeException.class,()->access.reveal(problem.getId(),current.getUsername()));e.setPublished(true);editorials.save(e);assertEquals("REVEALED",access.access(problem.getId(),current.getUsername()).status());}
 @Test void attemptsAreScopedAndOnlyEligibleVerdictsCount(){User other=newUser();Problem elsewhere=newProblem();for(int i=0;i<5;i++){submission(other,problem,"WRONG_ANSWER");submission(current,elsewhere,"WRONG_ANSWER");}for(String status:List.of("RUNNING","NO_TEST_CASES","EXECUTION_ERROR","LANGUAGE_NOT_SUPPORTED"))submission(current,problem,status);assertEquals(0,access.access(problem.getId(),current.getUsername()).failedSubmissionCount());for(String status:List.of("COMPILATION_ERROR","TIME_LIMIT_EXCEEDED","RUNTIME_ERROR","MEMORY_LIMIT_EXCEEDED"))submission(current,problem,status);assertEquals(4,access.access(problem.getId(),current.getUsername()).failedSubmissionCount());}
 @Test void concurrentRevealsCreateOneRecord()throws Exception{failures(3);parallel(()->access.reveal(problem.getId(),current.getUsername()));assertEquals(1,reveals.countByUserIdAndProblemId(current.getId(),problem.getId()));}
 @Test void thresholdsDistinctCountingAndBackfillAreIdempotent(){
  assertEquals(0,awards.evaluate(current.getId()).solvedCount());
  for(int i=0;i<49;i++)submission(current,newProblem(),"ACCEPTED");assertTrue(awards.evaluate(current.getId()).newlyIssued().isEmpty());
  submission(current,problem,"ACCEPTED");var fifty=awards.evaluate(current.getId());assertEquals(1,fifty.newlyIssued().size());assertEquals(50,fifty.currentMilestone());
  for(int i=0;i<3;i++){submission(current,problem,"ACCEPTED");submission(current,newProblem(),"WRONG_ANSWER");}
  assertEquals(50,awards.evaluate(current.getId()).solvedCount());assertTrue(awards.evaluate(current.getId()).newlyIssued().isEmpty());
  for(int i=50;i<53;i++)submission(current,newProblem(),"ACCEPTED");var progress=awards.progress(current.getId());assertEquals(List.of(50L,53L,53L),progress.milestones().stream().map(CertificateProgressResponse.MilestoneProgress::current).toList());assertEquals(List.of(0L,47L,97L),progress.milestones().stream().map(CertificateProgressResponse.MilestoneProgress::remaining).toList());
  for(int i=53;i<160;i++)submission(current,newProblem(),"ACCEPTED");var earned=awards.evaluate(current.getId());assertEquals(2,earned.newlyIssued().size());assertEquals(150,earned.currentMilestone());assertNull(earned.nextMilestone());assertEquals(3,certificates.findByUserIdOrderByMilestoneAsc(current.getId()).size());assertTrue(awards.evaluate(current.getId()).newlyIssued().isEmpty());
 }
 @Test void concurrentAwardsAreUniqueAndPublicVerificationIsSafe()throws Exception{
  for(int i=0;i<50;i++)submission(current,newProblem(),"ACCEPTED");parallel(()->awards.evaluate(current.getId()));
  var owned=certificates.findByUserIdOrderByMilestoneAsc(current.getId());assertEquals(1,owned.size());String publicId=owned.getFirst().getPublicCertificateId();UUID.fromString(publicId);
  mvc.perform(get("/api/certificates/{id}",publicId)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VERIFIED")).andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.user").doesNotExist()).andExpect(jsonPath("$.recipientName").value(current.getUsername()));
  mvc.perform(get("/api/certificates/{id}/pdf",publicId)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF));
  mvc.perform(get("/api/certificates/{id}",UUID.randomUUID())).andExpect(status().isNotFound());
 }
 @Test void userAdminPermissionsRemainSeparate()throws Exception{
  mvc.perform(get("/api/users/me/certificate-progress").with(user(current.getUsername()).roles("USER"))).andExpect(status().isOk()).andExpect(jsonPath("$.milestones.length()").value(3));
  mvc.perform(get("/api/admin/users/{id}/certificate-progress",current.getId()).with(user(current.getUsername()).roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/users/me/certificate-progress")).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/users/{id}/certificate-progress",current.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(get("/api/problems/{id}/editorial",problem.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.approach").value("SECRET APPROACH"));
  mvc.perform(put("/api/problems/{id}/editorial",problem.getId()).with(user(current.getUsername()).roles("USER")).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/certificates/summary").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").exists());
 }
 @Test void backfillPreservesCustomContentAndUnpublishedDefaults(){
  long before=editorials.count();assertEquals(0,backfill.backfill());assertEquals(before,editorials.count());
  Editorial seeded=editorials.findAll().stream().filter(e->"VERDIXA_CATALOG_V1".equals(e.getContentSource())).findFirst().orElseThrow();
  seeded.setApproach("My custom explanation");seeded.setPublished(false);editorials.save(seeded);backfill.backfill();Editorial unchanged=editorials.findById(seeded.getId()).orElseThrow();assertEquals("My custom explanation",unchanged.getApproach());assertFalse(unchanged.isPublished());
 }
 private void parallel(Callable<?> task)throws Exception{try(ExecutorService pool=Executors.newFixedThreadPool(2)){CountDownLatch start=new CountDownLatch(1);Callable<Object> work=()->{start.await();return task.call();};Future<?> a=pool.submit(work),b=pool.submit(work);start.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);}}
}
