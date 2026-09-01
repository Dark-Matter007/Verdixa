package com.leetcode.backend;

import com.leetcode.backend.model.ExecutionMode;
import com.leetcode.backend.model.FunctionParameter;
import com.leetcode.backend.model.FunctionSignature;
import com.leetcode.backend.model.Problem;
import com.leetcode.backend.model.Role;
import com.leetcode.backend.model.User;
import com.leetcode.backend.repository.ProblemRepository;
import com.leetcode.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regression coverage for the user library pagination contract. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:library;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters"})
@AutoConfigureMockMvc
class ProblemLibraryApiTest {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Autowired ProblemRepository problems;
 Long functionProblemId;

 @BeforeEach void seed(){
  if(users.findByUsername("library-user").isEmpty()) users.save(new User("library-user","library-user@test.dev","hash", Role.USER));
  Problem problem=new Problem();
  problem.setTitle("FUNCTION library regression"); problem.setDescription("A function-mode problem"); problem.setDifficulty("EASY"); problem.setTags("Strings"); problem.setExecutionMode(ExecutionMode.FUNCTION); problem.setActive(true);
  FunctionParameter parameter=new FunctionParameter(); parameter.setName("text"); parameter.setType("String"); parameter.setParameterOrder(0);
  FunctionSignature signature=new FunctionSignature(); signature.setFunctionName("reverse"); signature.setReturnType("String"); signature.setParameters(List.of(parameter));
  problem.setFunctionSignature(signature); functionProblemId=problems.save(problem).getId();
 }

 @Test @WithMockUser(username="library-user",roles="USER") void paginatedLibraryDoesNotSerializeFunctionConfiguration() throws Exception {
  mvc.perform(get("/api/problems/library").param("search","FUNCTION library regression"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.content").isArray())
    .andExpect(jsonPath("$.content[0].problem.title").value("FUNCTION library regression"))
    .andExpect(jsonPath("$.content[0].problem.executionMode").value("FUNCTION"))
    .andExpect(jsonPath("$.content[0].problem.functionSignature").doesNotExist());
 }

 @Test @WithMockUser(username="library-user",roles="USER") void detailMaterializesFunctionSignatureWithoutHiddenTests() throws Exception {
  mvc.perform(get("/api/problems/{id}",functionProblemId))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.title").value("FUNCTION library regression"))
    .andExpect(jsonPath("$.executionMode").value("FUNCTION"))
    .andExpect(jsonPath("$.functionSignature.functionName").value("reverse"))
    .andExpect(jsonPath("$.functionSignature.parameters[0].name").value("text"))
    .andExpect(jsonPath("$.functionSignature.parameters[0].order").value(0))
    .andExpect(jsonPath("$.testCases").doesNotExist())
    .andExpect(jsonPath("$.hiddenTestCases").doesNotExist());
 }

 @Test @WithMockUser(username="library-user",roles="USER") void inactiveDetailIsNotVisibleToUser() throws Exception {
  Problem inactive=new Problem(); inactive.setTitle("Private problem"); inactive.setDescription("d"); inactive.setDifficulty("EASY"); inactive.setActive(false);
  Long inactiveId=problems.save(inactive).getId();
  mvc.perform(get("/api/problems/{id}",inactiveId)).andExpect(status().isNotFound());
 }
}
