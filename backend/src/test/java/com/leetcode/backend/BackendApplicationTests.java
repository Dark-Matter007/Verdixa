package com.leetcode.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:algosphere;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"
})
@AutoConfigureMockMvc
class BackendApplicationTests {
	@Autowired MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotRetrieveAllHiddenTestCases() throws Exception {
		mockMvc.perform(get("/api/problems/1/testcases/all")).andExpect(status().isForbidden());
	}

}
