package com.leetcode.backend;

import com.leetcode.backend.model.*;
import com.leetcode.backend.repository.ProblemRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:adminupdate;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop",
        "algosphere.jwt.secret=test-only-secret-that-is-at-least-32-characters",
        "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
class AdminProblemUpdateIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ProblemRepository problems;
    Problem functionProblem;
    List<Long> originalParameterIds;

    @BeforeEach
    void seed() {
        Problem problem = new Problem(); problem.setTitle("Function update " + System.nanoTime());
        problem.setDescription("before"); problem.setDifficulty("EASY"); problem.setStarterCode("starter-before");
        problem.setExecutionMode(ExecutionMode.FUNCTION); problem.setActive(false);
        FunctionSignature signature = new FunctionSignature(); signature.setFunctionName("combine"); signature.setReturnType("String");
        signature.setParameters(new ArrayList<>(List.of(parameter("left", "String", 0), parameter("right", "String", 1))));
        problem.setFunctionSignature(signature); functionProblem = problems.saveAndFlush(problem);
        originalParameterIds = problems.findWithFunctionSignatureById(functionProblem.getId()).orElseThrow()
                .getFunctionSignature().getParameters().stream().map(FunctionParameter::getId).toList();
    }

    @Test
    @WithMockUser(roles="ADMIN")
    void functionProblemLoadsAndUpdatesWithoutLazyInitialization() throws Exception {
        mvc.perform(get("/api/problems/{id}", functionProblem.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.functionSignature.parameters[0].name").value("left"));
        String body = """
                {"title":"Function updated","description":"after","difficulty":"MEDIUM","starterCode":"starter-after",
                 "executionMode":"FUNCTION","active":false,"functionSignature":{"functionName":"join","returnType":"String",
                 "parameters":[{"name":"first","type":"String","parameterOrder":1},{"name":"count","type":"int","parameterOrder":0}]}}
                """;
        mvc.perform(put("/api/problems/{id}", functionProblem.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Function updated"))
                .andExpect(jsonPath("$.starterCode").value("starter-after"))
                .andExpect(jsonPath("$.functionSignature.functionName").value("join"))
                .andExpect(jsonPath("$.functionSignature.parameters[0].name").value("first"))
                .andExpect(jsonPath("$.functionSignature.parameters[0].order").value(0))
                .andExpect(jsonPath("$.functionSignature.parameters[1].name").value("count"))
                .andExpect(jsonPath("$.functionSignature.parameters[1].order").value(1));
        Problem stored = problems.findWithFunctionSignatureById(functionProblem.getId()).orElseThrow();
        assertEquals(originalParameterIds, stored.getFunctionSignature().getParameters().stream().map(FunctionParameter::getId).toList());
        assertEquals(List.of("first", "count"), stored.getFunctionSignature().getParameters().stream().map(FunctionParameter::getName).toList());
    }

    @Test
    @WithMockUser(roles="ADMIN")
    void stdinProblemUpdateReturnsDtoWithoutFunctionSignature() throws Exception {
        Problem stdin = new Problem(); stdin.setTitle("STDIN " + System.nanoTime()); stdin.setDescription("before");
        stdin.setDifficulty("EASY"); stdin.setExecutionMode(ExecutionMode.STDIN); stdin.setActive(false); stdin = problems.saveAndFlush(stdin);
        String body = "{\"title\":\"STDIN updated\",\"description\":\"after\",\"difficulty\":\"EASY\",\"executionMode\":\"STDIN\",\"active\":false}";
        mvc.perform(put("/api/problems/{id}", stdin.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.executionMode").value("STDIN"))
                .andExpect(jsonPath("$.functionSignature").doesNotExist());
    }

    @Test
    @WithMockUser(roles="USER")
    void userCannotUpdateProblem() throws Exception {
        mvc.perform(put("/api/problems/{id}", functionProblem.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{}" )).andExpect(status().isForbidden());
    }

    private FunctionParameter parameter(String name, String type, int order) {
        FunctionParameter parameter = new FunctionParameter(); parameter.setName(name); parameter.setType(type);
        parameter.setParameterOrder(order); return parameter;
    }
}
