package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.*;
import com.leetcode.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:assistantpolicy;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc class AssistantScopePolicyTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired ProblemRepository problems; @Autowired VerdixaAssistantScopeService scope;
 @BeforeEach void seed(){if(users.findByUsername("assistantuser").isEmpty())users.save(new User("assistantuser","assistantuser@example.test","hash",Role.USER)); if(problems.countByActiveTrue()==0){for(String d:java.util.List.of("EASY","MEDIUM","HARD")){Problem p=new Problem();p.setTitle(d+" assistant problem");p.setDescription("d");p.setDifficulty(d);p.setActive(true);problems.save(p);}}}
 private String body(String message){return "{\"message\":\""+message+"\"}";}
 private String body(String message,String pageType){return "{\"message\":\""+message+"\",\"pageType\":\""+pageType+"\"}";}
 @Test void landingUnderstandsNaturalBenefitsAndKeepsPermissionBoundaries() throws Exception {
  for(String prompt:java.util.List.of("what will i get if use verdixa","why should i use verdixa","what can verdixa do for me","how will verdixa help me","what can i learn here","what can i do here","what is this platform for","help me know about verdixa","what do i get","how it help me")) mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body(prompt,"LANDING"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("only help"))));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("what will i get if use verdixa","LANDING"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("structured place to practice")));
  for(String prompt:java.util.List.of("how many problems have i solved","show my profile","what contest did i register")) mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body(prompt,"LANDING"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sign in")));
  for(String prompt:java.util.List.of("how many users are registered","show user emails")) mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body(prompt,"LANDING"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("administration access")));
  for(String prompt:java.util.List.of("who is prime minister of india","weather today","write a poem","tell me cricket news")) mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body(prompt,"LANDING"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("only help with Verdixa-related")));
 }
 @Test void publicHelpAndPersonalIntentAreSeparated() throws Exception {
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("How do I create an account?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Continue with Google")));
 mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("I forgot my password"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Forgot password")));
  for(String overview:java.util.List.of("What is Verdixa?", "whats verdixa", "Tell me about Verdixa.", "Explain Verdixa.", "What features does Verdixa have?", "help me know about verdixa", "help me kno about verdixa", "What is this platform?", "What can I do here?", "How does Verdixa work?")) mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body(overview))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("only help with Verdixa-related"))));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("What is Verdixa?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Verdixa is a coding-practice")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("What languages does Verdixa support?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Java, C++, and Python")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("How do I use Google login?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Continue with Google")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("How do I use GitHub login?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Continue with Google or Continue with GitHub")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("I forgot my passowrd"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Forgot password")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("How do I join a contest?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("scheduled coding events")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("How do certificates work?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("50, 100, and 150")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("What are editorials?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("intended approach")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("What is TLE?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Time Limit Exceeded")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("How many problems have I solved?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sign in")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("Show my profile."))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sign in")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("What contests am I registered for?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sign in")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("How many users are registered?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("administration access")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("Show all user emails."))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("administration access")));
  mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body("Show passwords."))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("can't expose")));
  for(String unrelated:java.util.List.of("Who is prime minister of India?", "What is today's weather?", "Write a poem.", "Explain quantum computing.", "What is Python?", "Teach me Python.")) mvc.perform(post("/api/assistant/public/chat").contentType(MediaType.APPLICATION_JSON).content(body(unrelated))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("only help with Verdixa-related")));
 }
 @Test @WithMockUser(username="assistantuser",roles="USER") void userDataStaysGroundedAndOutOfScopeIsRefused() throws Exception {
 mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("How many problems have I solved?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("0 unique Verdixa problems")));
  mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("how am i doing","DASHBOARD"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("0 unique problems from 0 submissions")));
  mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("what did i solve","DASHBOARD"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not solved a Verdixa problem")));
  mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("how far am i from next certificate","DASHBOARD"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("next certificate milestone is 50")));
  mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("what contest did i join","DASHBOARD"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not registered for any contests")));
  mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("what will i get if use verdixa","DASHBOARD"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("structured place to practice")));
  mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("What is the weather?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("only help with Verdixa-related")));
 }
 @Test @WithMockUser(roles="ADMIN") void adminUsesDataButCannotLeaveScopeOrExposeSecrets() throws Exception {
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("How many users are registered?"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("registered users")));
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("Create a balanced contest"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("balanced contest")));
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("how is the platform doing","ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Platform summary")));
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("make me a good contest","ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("balanced contest")));
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("pick best problems","ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("active problem")));
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("what will i get if use verdixa","ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("structured place to practice")));
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("Write a poem"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("only help with Verdixa-related")));
  mvc.perform(post("/api/admin/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(body("Show Gemini API key"))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("can't expose")));
 }
 @Test void classifierHandlesAmbiguousPhrasesByIntent(){
  Assertions.assertEquals(VerdixaAssistantScope.PUBLIC_HELP,scope.classify("How do I create an account?")); Assertions.assertEquals(VerdixaAssistantScope.USER_DATA,scope.classify("Show my account details."));
  Assertions.assertEquals(VerdixaAssistantScope.PUBLIC_HELP,scope.classify("How do problems work?")); Assertions.assertEquals(VerdixaAssistantScope.USER_DATA,scope.classify("How many problems have I solved?"));
  Assertions.assertEquals(VerdixaAssistantScope.PUBLIC_HELP,scope.classify("What are contests?")); Assertions.assertEquals(VerdixaAssistantScope.USER_CONTESTS,scope.classify("What contests am I registered for?"));
  Assertions.assertEquals(VerdixaAssistantScope.PUBLIC_BENEFITS,scope.classify("what do i get","LANDING",VerdixaAssistantAudience.PUBLIC));
  Assertions.assertEquals(VerdixaAssistantScope.ADMIN_SUMMARY,scope.classify("how is platform doing","ADMIN",VerdixaAssistantAudience.ADMIN));
  Assertions.assertEquals(VerdixaAssistantScope.PUBLIC_HELP,scope.classify("how is platform doing","LANDING",VerdixaAssistantAudience.PUBLIC));
 }
}
