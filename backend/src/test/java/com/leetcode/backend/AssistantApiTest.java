package com.leetcode.backend;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:assistant;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"}) @AutoConfigureMockMvc class AssistantApiTest {
 @Autowired MockMvc mvc;
 @Test void publicAssistantIsAnonymousButNeverReturnsPersonalData() throws Exception { mvc.perform(post("/api/assistant/public/chat").contentType("application/json").content("{\"message\":\"How many problems have I solved?\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sign in to Verdixa"))); mvc.perform(post("/api/assistant/chat").contentType("application/json").content("{\"message\":\"How many problems have I solved?\"}")).andExpect(status().isForbidden()); }
}
